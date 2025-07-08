package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.util.*;

import ij.ImageStack;
import ij.gui.Roi;
import jakarta.annotation.PostConstruct;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.filter.*;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.model.FilterEnum;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.service.bean.LswImageLayers;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.PsfDiskGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.plugin.Scaler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;

import static nl.wilcokas.luckystackworker.util.LswImageProcessingUtil.getLswImageLayers;

@Slf4j
@RequiredArgsConstructor
@Service
public class FilterService {

    private final LSWSharpenFilter lswSharpenFilter;
    private final RGBBalanceFilter rgbBalanceFilter;
    private final SaturationFilter saturationFilter;
    private final SavitzkyGolayFilter savitzkyGolayFilter;
    private final SigmaDenoise1Filter sigmaDenoise1Filter;
    private final SigmaDenoise2Filter sigmaDenoise2Filter;
    private final DispersionCorrectionFilter dispersionCorrectionFilter;
    private final IansNoiseReductionFilter iansNoiseReductionFilter;
    private final EqualizeLocalHistogramsFilter equalizeLocalHistogramsFilter;
    private final ColorNormalisationFilter colorNormalisationFilter;
    private final HistogramStretchFilter histogramStretchFilter;
    private final WienerDeconvolutionFilter wienerDeconvolutionFilter;
    private final BilateralDenoiseFilter bilateralDenoiseFilter;
    private final LocalContrastFilter localContrastFilter;
    private final RotationFilter rotationFilter;
    private final GammaFilter gammaFilter;
    private final ClippingSuppressionFilter clippingSuppressionFilter;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    private int displayedProgress = 0;
    private Timer timer = new Timer();

    private final List<Pair<FilterEnum, LSWFilter>> filters = new ArrayList<>();

    private Map<FilterEnum, LswImageLayers> cache = new HashMap<>();

    @PostConstruct
    void init() {
        filters.add(Pair.of(FilterEnum.CLIPPING_SUPPRESSION, clippingSuppressionFilter));
        filters.add(Pair.of(FilterEnum.WIENER_DECONV, wienerDeconvolutionFilter));
        filters.add(Pair.of(FilterEnum.SHARPEN, lswSharpenFilter));
        filters.add(Pair.of(FilterEnum.ROTATE, rotationFilter));
        filters.add(Pair.of(FilterEnum.SIGMA_DENOISE_1, sigmaDenoise1Filter));
        filters.add(Pair.of(FilterEnum.IANS_NR, iansNoiseReductionFilter));
        filters.add(Pair.of(FilterEnum.BILATERAL_DENOISE, bilateralDenoiseFilter));
        filters.add(Pair.of(FilterEnum.SIGMA_DENOISE_2, sigmaDenoise2Filter));
        filters.add(Pair.of(FilterEnum.SAVITSKY_GOLAY, savitzkyGolayFilter));
        filters.add(Pair.of(FilterEnum.EQUALIZE_LOCALLY, equalizeLocalHistogramsFilter));
        filters.add(Pair.of(FilterEnum.LOCAL_CONTRAST, localContrastFilter));
        filters.add(Pair.of(FilterEnum.GAMMA, gammaFilter));
        filters.add(Pair.of(FilterEnum.COLOR_NORMALIZE, colorNormalisationFilter));
        filters.add(Pair.of(FilterEnum.RGB_BALANCE, rgbBalanceFilter));
        filters.add(Pair.of(FilterEnum.SATURATION, saturationFilter));
        filters.add(Pair.of(FilterEnum.DISPERSION, dispersionCorrectionFilter));
        filters.add(Pair.of(FilterEnum.HISTOGRAM_STRETCH, histogramStretchFilter));
    }

    public void correctExposure(ImagePlus image) {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public void clearCache() {
        cache.clear();
    }

    public byte[] applyAllFilters(ImagePlus image, LswImageViewer viewer, Profile profile, List<FilterEnum> filterParams, boolean isMono) {
        updateProgress(viewer, 0, false);

        // Sharpening filters
        byte[] psfImage = updatePSF(profile.getPsf(), filterParams, profile.getName(), isMono);
        List<FilterEnum> appliedFilters = new ArrayList<>(filterParams);
        if (psfImage != null) {
            appliedFilters.add(FilterEnum.WIENER_DECONV);
        }
        int progressIncrease = 100 / this.filters.size();
        int progress = 0;
        boolean filterEncountered = false;
        FilterEnum previousCachedOperation = getPreviousCachedOperation(profile, image, appliedFilters);

        ImageStack stack = image.getStack();
        ImagePlus workImage = image;
        Roi roi = luckyStackWorkerContext.getSelectedRoi();
        if (luckyStackWorkerContext.isRoiActive()) {
            workImage = createTempCroppedImage(roi, stack);
        }

        int cacheWidth = cache.isEmpty() ? 0 : cache.entrySet().stream().findFirst().get().getValue().getWidth();
        int cacheHeight = cache.isEmpty() ? 0 : cache.entrySet().stream().findFirst().get().getValue().getHeight();
        if (workImage.getHeight() != cacheHeight || workImage.getWidth() != cacheWidth) {
            log.info("ROI has been modified, clearing the cache");
            clearCache();
        }

        for (int i = 0; i < this.filters.size(); i++) {
            Pair<FilterEnum, LSWFilter> filterData = this.filters.get(i);
            FilterEnum filterOperation = filterData.getLeft();
            LswImageLayers cacheValue = cache.get(filterOperation);
            if (appliedFilters.contains(filterOperation) || filterEncountered || appliedFilters.isEmpty() || cacheValue == null) {
                LSWFilter filter = filterData.getRight();
                if (filter.apply(workImage, profile, isMono)) {
                    cache.put(filterOperation, LswImageProcessingUtil.getImageLayers(workImage));
                }
                filterEncountered = true;
            } else if (previousCachedOperation == filterOperation) {
                log.info("Applying {} from cache", filterOperation);
                LswImageProcessingUtil.updateImageLayers(workImage, cacheValue);
            }
            progress += progressIncrease;
            boolean nextOperationSlow = this.filters.get(i < this.filters.size() - 1 ? i + 1 : i).getRight().isSlow();
            updateProgress(viewer, progress, nextOperationSlow);
        }

        if (luckyStackWorkerContext.isRoiActive()) {
            copyPixelsBackToImage(roi, workImage.getStack(), stack);
        }

        Timer resetProgressTimer = new Timer();
        resetProgressTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                displayedProgress = 0;
                resetProgressTimer.cancel();
                if (viewer != null) {
                    viewer.updateProgress(displayedProgress);
                }
            }
        }, Constants.ARTIFICIAL_PROGRESS_DELAY, Constants.ARTIFICIAL_PROGRESS_DELAY);
        if (appliedFilters.contains(FilterEnum.WIENER_DECONV)) {
            psfImage = LswFileUtil.getWienerDeconvolutionPSFImage(profile.getName());
        }
        return psfImage;
    }

    public ImagePlus scaleImage(final ImagePlus image, final double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        int depth = image.getStack().size();
        return Scaler.resize(image, newWidth, newHeight, depth, "depth=%s interpolation=Bicubic create".formatted(depth));
    }

    private ImagePlus createTempCroppedImage(Roi roi, ImageStack stack) {
        int roiWidth = (int) roi.getFloatWidth();
        int roiHeight = (int) roi.getFloatHeight();
        short[][] newPixels = new short[3][roiWidth * roiHeight];
        int xOffset = (int) roi.getXBase();
        int yOffset = (int) roi.getYBase();
        int imageWidth = stack.getWidth();
        for (int y = 0; y < roiHeight; y++) {
            for (int x = 0; x < roiWidth; x++) {
                short[] ipRedPixels = (short[]) (stack.getProcessor(1).getPixels());
                short[] ipGreenPixels = (short[]) (stack.getProcessor(2).getPixels());
                short[] ipBluePixels = (short[]) (stack.getProcessor(3).getPixels());
                int xOrg = x + xOffset;
                int yOrg = y + yOffset;
                newPixels[0][x + y * roiWidth] = ipRedPixels[xOrg + yOrg * imageWidth];
                newPixels[1][x + y * roiWidth] = ipGreenPixels[xOrg + yOrg * imageWidth];
                newPixels[2][x + y * roiWidth] = ipBluePixels[xOrg + yOrg * imageWidth];
            }
        }
        return LswImageProcessingUtil.create16BitRGBImage("crop",
                getLswImageLayers(newPixels, roiWidth, roiHeight), true, true, true);
    }

    private void copyPixelsBackToImage(Roi roi, ImageStack tempCroppedStack, ImageStack stack) {
        short[] roiRedPixels = (short[]) (tempCroppedStack.getProcessor(1).getPixels());
        short[] roiGreenPixels = (short[]) (tempCroppedStack.getProcessor(2).getPixels());
        short[] roiBluePixels = (short[]) (tempCroppedStack.getProcessor(3).getPixels());
        int roiWidth = (int) roi.getFloatWidth();
        int roiHeight = (int) roi.getFloatHeight();
        int xOffset = (int) roi.getXBase();
        int yOffset = (int) roi.getYBase();
        int imageWidth = stack.getWidth();
        for (int y = 0; y < roiHeight; y++) {
            for (int x = 0; x < roiWidth; x++) {
                short[] imageRedPixels = (short[]) stack.getProcessor(1).getPixels();
                short[] imageGreenPixels = (short[]) stack.getProcessor(2).getPixels();
                short[] imageBluePixels = (short[]) stack.getProcessor(3).getPixels();
                int xOrg = x + xOffset;
                int yOrg = y + yOffset;
                imageRedPixels[xOrg + yOrg * imageWidth] = roiRedPixels[x + y * roiWidth];
                imageGreenPixels[xOrg + yOrg * imageWidth] = roiGreenPixels[x + y * roiWidth];
                imageBluePixels[xOrg + yOrg * imageWidth] = roiBluePixels[x + y * roiWidth];
            }
        }
    }

    private FilterEnum getPreviousCachedOperation(Profile profile, ImagePlus image, List<FilterEnum> operations) {
        boolean operationFound = false;
        for (int i = filters.size() - 1; i >= 0; i--) {
            Pair<FilterEnum, LSWFilter> filterData = filters.get(i);
            FilterEnum currentFilterOperation = filterData.getLeft();
            if (operations.contains(currentFilterOperation)) {
                operationFound = true;
                continue;
            }
            LswImageLayers cacheValue = cache.get(currentFilterOperation);
            if (operationFound && cacheValue != null && filterData.getRight().isApplied(profile, image)) {
                return currentFilterOperation;
            }
        }
        return null;
    }

    private void updateProgress(LswImageViewer viewer, int progress, boolean slowOperationNext) {
        if (displayedProgress < progress) {
            displayedProgress = progress;
        }
        if (viewer != null) {
            viewer.updateProgress(displayedProgress);
        }
        if (slowOperationNext) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (viewer != null) {
                        viewer.updateProgress(displayedProgress++);
                    }
                }
            }, Constants.ARTIFICIAL_PROGRESS_DELAY, Constants.ARTIFICIAL_PROGRESS_DELAY);
        } else {
            timer.cancel();
        }
    }

    private byte[] updatePSF(PSF psf, List<FilterEnum> operations, String profileName, boolean isMono) {
        if (operations.contains(FilterEnum.PSF)) {
            try {
                PsfDiskGenerator.generate16BitRGB(psf.getAiryDiskRadius(), psf.getSeeingIndex(), psf.getDiffractionIntensity(), profileName, isMono);
            } catch (IOException e) {
                throw new FilterException(e.getMessage());
            }
            psf.setType(PSFType.SYNTHETIC);
            return LswFileUtil.getWienerDeconvolutionPSFImage(profileName);
        }
        return null;
    }

}
