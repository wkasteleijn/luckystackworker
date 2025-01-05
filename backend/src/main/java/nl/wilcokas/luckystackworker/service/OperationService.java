package nl.wilcokas.luckystackworker.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import nl.wilcokas.luckystackworker.filter.*;
import nl.wilcokas.luckystackworker.filter.settings.*;
import nl.wilcokas.luckystackworker.filter.sigma.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.PsfDiskGenerator;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import ij.plugin.Scaler;
import ij.process.ImageProcessor;
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
    private final GammaFilter gammaFilter;

    private int displayedProgress = 0;
    private Timer timer = new Timer();

    // TODO: create init method to fill the ordered map in the correct order.
    private static Map<OperationEnum,LSWFilter> filterMap = new LinkedHashMap();

    public void correctExposure(ImagePlus image) throws IOException {
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
    }

    public byte[] applyAllOperations(ImagePlus image, LswImageViewer viewer, Profile profile, OperationEnum operation, boolean isMono) throws IOException, InterruptedException {
        updateProgress(viewer, 0, true);

        // Sharpening filters
        byte[] psfImage = updatePSF(profile.getPsf(), operation, profile.getName(), isMono);
        wienerDeconvolutionFilter.apply(image, profile, isMono);
        lswSharpenFilter.apply(image, profile, isMono);
        updateProgress(viewer, 9);

        // Denoise filters -- pass 1
        sigmaDenoise1Filter.apply(image, profile, isMono);
        iansNoiseReductionFilter.apply(image, profile, isMono);
        bilateralDenoiseFilter.apply(image, profile, isMono);
        updateProgress(viewer, 18);

        // Denoise filters -- pass 2
        sigmaDenoise2Filter.apply(image, profile, isMono);
        savitzkyGolayFilter.apply(image, profile, isMono);
        updateProgress(viewer, 27, true);

        // Light and contrast filters
        equalizeLocalHistogramsFilter.apply(image, profile, isMono);
        updateProgress(viewer, 36);
        localContrastFilter.apply(image, profile, isMono);
        updateProgress(viewer, 45);
        gammaFilter.apply(image, profile, isMono);
        updateProgress(viewer, 54);

        // Color filters
        colorNormalisationFilter.apply(image, profile, isMono);
        updateProgress(viewer, 63);
        rgbBalanceFilter.apply(image, profile, isMono);
        updateProgress(viewer, 72);
        saturationFilter.apply(image, profile, isMono);
        updateProgress(viewer, 81);
        dispersionCorrectionFilter.apply(image, profile, isMono);
        updateProgress(viewer, 90);

        // Always apply last
        histogramStretchFilter.apply(image, profile, isMono);
        updateProgress(viewer, 100);

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
        return psfImage;
    }

    public ImagePlus scaleImage(final ImagePlus image, final double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        int depth = image.getStack().size();
        return Scaler.resize(image, newWidth, newHeight, depth, "depth=%s interpolation=Bicubic create".formatted(depth));
    }

    private void updateProgress(LswImageViewer viewer, int progress) {
        updateProgress(viewer, progress, false);
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

    private byte[] updatePSF(PSF psf, OperationEnum operation, String profileName, boolean isMono) throws IOException {
        if (operation == OperationEnum.PSF) {
            PsfDiskGenerator.generate16BitRGB(psf.getAiryDiskRadius(), psf.getSeeingIndex(), psf.getDiffractionIntensity(), profileName, isMono);
            psf.setType(PSFType.SYNTHETIC);
            return LswFileUtil.getWienerDeconvolutionPSFImage(profileName);
        }
        return null;
    }

}
