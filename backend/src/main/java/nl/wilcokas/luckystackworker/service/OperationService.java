package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;

import ij.ImageStack;
import jakarta.annotation.PostConstruct;
import nl.wilcokas.luckystackworker.filter.*;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class OperationService {

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

    private int displayedProgress = 0;
    private Timer timer = new Timer();

    private final List<Pair<OperationEnum, LSWFilter>> filters = new ArrayList<>();
    private Map<OperationEnum, LswImageLayersDto> cache = new HashMap<>();

    @PostConstruct
    void init() {
        filters.add(Pair.of(OperationEnum.WIENER_DECONV, wienerDeconvolutionFilter));
        filters.add(Pair.of(OperationEnum.SHARPEN, lswSharpenFilter));
        filters.add(Pair.of(OperationEnum.ROTATE, rotationFilter));
        filters.add(Pair.of(OperationEnum.SIGMA_DENOISE_1, sigmaDenoise1Filter));
        filters.add(Pair.of(OperationEnum.IANS_NR, iansNoiseReductionFilter));
        filters.add(Pair.of(OperationEnum.BILATERAL_DENOISE, bilateralDenoiseFilter));
        filters.add(Pair.of(OperationEnum.SIGMA_DENOISE_2, sigmaDenoise2Filter));
        filters.add(Pair.of(OperationEnum.SAVITSKY_GOLAY, savitzkyGolayFilter));
        filters.add(Pair.of(OperationEnum.EQUALIZE_LOCALLY, equalizeLocalHistogramsFilter));
        filters.add(Pair.of(OperationEnum.LOCAL_CONTRAST, localContrastFilter));
        filters.add(Pair.of(OperationEnum.GAMMA, gammaFilter));
        filters.add(Pair.of(OperationEnum.COLOR_NORMALIZE, colorNormalisationFilter));
        filters.add(Pair.of(OperationEnum.RGB_BALANCE, rgbBalanceFilter));
        filters.add(Pair.of(OperationEnum.SATURATION, saturationFilter));
        filters.add(Pair.of(OperationEnum.DISPERSION, dispersionCorrectionFilter));
        filters.add(Pair.of(OperationEnum.HISTOGRAM_STRETCH, histogramStretchFilter));
    }

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public void clearCache() {
        cache.clear();
    }

    public byte[] applyAllOperations(ImagePlus image, LswImageViewer viewer, Profile profile, List<OperationEnum> operationParams, boolean isMono) throws IOException, InterruptedException {
        updateProgress(viewer, 0, true);

        // Sharpening filters
        byte[] psfImage = updatePSF(profile.getPsf(), operationParams, profile.getName(), isMono);
        List<OperationEnum> operations = new ArrayList<>(operationParams);
        if (psfImage != null) {
            operations.add(OperationEnum.WIENER_DECONV);
        }
        int progressIncrease = 100 / filters.size();
        int progress = 0;
        boolean filterEncountered = false;
        OperationEnum previousCachedOperation = getPreviousCachedOperation(profile, image, operations);
        for (int i = 0; i < filters.size(); i++) {
            Pair<OperationEnum, LSWFilter> filterData = filters.get(i);
            OperationEnum filterOperation = filterData.getLeft();
            LswImageLayersDto cacheValue = cache.get(filterOperation);
            if (operations.contains(filterOperation) || filterEncountered || operations.isEmpty()) {
                LSWFilter filter = filterData.getRight();
                if (filter.apply(image, profile, isMono)) {
                    cache.put(filterOperation, LswImageProcessingUtil.getImageLayers(image));
                }
                filterEncountered = true;
            } else if (previousCachedOperation == filterOperation && cacheValue != null) {
                log.info("Applying {} from cache", filterOperation);
                LswImageProcessingUtil.updateImageLayers(image, cacheValue);
            }
            progress += progressIncrease;
            boolean nextOperationSlow = filters.get(i < filters.size() - 1 ? i + 1 : i).getRight().isSlow();
            updateProgress(viewer, progress, nextOperationSlow);
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
        if (operations.contains(OperationEnum.WIENER_DECONV)) {
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

    private OperationEnum getPreviousCachedOperation(Profile profile, ImagePlus image, List<OperationEnum> operations) {
        boolean operationFound = false;
        for (int i = filters.size() - 1; i >= 0; i--) {
            Pair<OperationEnum, LSWFilter> filterData = filters.get(i);
            OperationEnum currentFilterOperation = filterData.getLeft();
            if (operations.contains(currentFilterOperation)) {
                operationFound = true;
                continue;
            }
            LswImageLayersDto cacheValue = cache.get(currentFilterOperation);
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

    private byte[] updatePSF(PSF psf, List<OperationEnum> operations, String profileName, boolean isMono) throws IOException {
        if (operations.contains(OperationEnum.PSF)) {
            PsfDiskGenerator.generate16BitRGB(psf.getAiryDiskRadius(), psf.getSeeingIndex(), psf.getDiffractionIntensity(), profileName, isMono);
            psf.setType(PSFType.SYNTHETIC);
            return LswFileUtil.getWienerDeconvolutionPSFImage(profileName);
        }
        return null;
    }

}
