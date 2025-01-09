package nl.wilcokas.luckystackworker.filter;

import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Scaler;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.settings.LSWSharpenMode;
import nl.wilcokas.luckystackworker.filter.settings.WienerDeconvolutionParameters;
import nl.wilcokas.luckystackworker.filter.wpl.LswWPLFloatIterativeDeconvolver2D;
import nl.wilcokas.luckystackworker.model.PSF;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.PsfDiskGenerator;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class WienerDeconvolutionFilter implements LSWFilter {

    @Override
    public boolean apply(final ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (profile.getApplyWienerDeconvolution().booleanValue()) {
            log.info("Applying Wiener deconvolution filter");
            float deringStrength = profile.getDeringStrength() / 100f;
            float blendRaw = profile.getBlendRaw() / 100f;
            int iterations = profile.getWienerIterations() == 0 ? 1 : profile.getWienerIterations();
            WienerDeconvolutionParameters parameters;
            LSWSharpenMode mode = (profile.getSharpenMode() == null) ? LSWSharpenMode.LUMINANCE : LSWSharpenMode.valueOf(profile.getSharpenMode());
            if (mode == LSWSharpenMode.LUMINANCE) {
                parameters = WienerDeconvolutionParameters.builder()
                        .iterationsLuminance(iterations)
                        .deringRadiusLuminance(profile.getDeringRadius().doubleValue())
                        .deringStrengthLuminance(deringStrength)
                        .blendRawLuminance(blendRaw)
                        .mode(LSWSharpenMode.LUMINANCE)
                        .includeRed(profile.isLuminanceIncludeRed())
                        .includeGreen(profile.isLuminanceIncludeGreen())
                        .includeBlue(profile.isLuminanceIncludeBlue())
                        .includeColor(profile.isLuminanceIncludeColor())
                        .build();
            } else {
                int iterationsGreen = profile.getWienerIterationsGreen() == 0 ? 1 : profile.getWienerIterationsGreen();
                int iterationsBlue = profile.getWienerIterationsBlue() == 0 ? 1 : profile.getWienerIterationsBlue();
                float deringStrengthGreen = profile.getDeringStrengthGreen() / 100f;
                float deringStrengthBlue = profile.getDeringStrengthBlue() / 100f;
                float blendRawGreen = profile.getBlendRawGreen() / 100f;
                float blendRawBlue = profile.getBlendRawBlue() / 100f;
                parameters = WienerDeconvolutionParameters.builder()
                        .iterationsRed(iterations)
                        .deringRadiusRed(profile.getDeringRadius().doubleValue())
                        .deringStrengthRed(deringStrength)
                        .iterationsGreen(iterationsGreen)
                        .deringRadiusGreen(profile.getDeringRadiusGreen().doubleValue())
                        .deringStrengthGreen(deringStrengthGreen)
                        .iterationsBlue(iterationsBlue)
                        .deringRadiusBlue(profile.getDeringRadiusBlue().doubleValue())
                        .deringStrengthBlue(deringStrengthBlue)
                        .blendRawRed(blendRaw)
                        .blendRawGreen(blendRawGreen)
                        .blendRawBlue(blendRawBlue)
                        .mode(LSWSharpenMode.RGB)
                        .build();
            }

            if (mode == LSWSharpenMode.LUMINANCE && !validateLuminanceInclusion(parameters)) {
                log.warn("Attempt to exclude all channels from luminance sharpen!");
                parameters.setIncludeRed(true);
                parameters.setIncludeGreen(true);
                parameters.setIncludeBlue(true);
            }
            ImagePlus psfImage = LswFileUtil.getWienerDeconvolutionPSF(profile.getName());
            if (psfImage == null) {
                PSF psf = profile.getPsf();
                psf.setType(PSFType.SYNTHETIC);
                psfImage = PsfDiskGenerator.generate16BitRGB(psf.getAiryDiskRadius(), psf.getSeeingIndex(), psf.getDiffractionIntensity(), profile.getName(), isMono);
            }
            apply(image, psfImage, parameters);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    private boolean validateLuminanceInclusion(WienerDeconvolutionParameters parameters) {
        return parameters.isIncludeRed() || parameters.isIncludeGreen() || parameters.isIncludeBlue();
    }

    private void apply(ImagePlus image, ImagePlus psf, WienerDeconvolutionParameters parameters) {
        ImagePlus[] psfPerChannel = getPsfPerChannel(psf);
        ImageStack stack = image.getStack();
        if (parameters.getMode() == LSWSharpenMode.RGB) {
            applyToChannel(stack.getProcessor(1), psfPerChannel[0], parameters.getIterationsRed(), parameters.getDeringStrengthRed(), parameters.getDeringRadiusRed(), parameters.getBlendRawRed());
            applyToChannel(stack.getProcessor(2), psfPerChannel[1], parameters.getIterationsGreen(), parameters.getDeringStrengthGreen(), parameters.getDeringRadiusGreen(), parameters.getBlendRawGreen());
            applyToChannel(stack.getProcessor(3), psfPerChannel[2], parameters.getIterationsBlue(), parameters.getDeringStrengthBlue(), parameters.getDeringRadiusBlue(), parameters.getBlendRawBlue());
        } else {
            applyLuminance(image, psf, parameters);
        }
    }

    private ImagePlus[] getPsfPerChannel(ImagePlus psf) {
        ImageStack stack = psf.getStack();
        ImagePlus[] psfPerChannel = new ImagePlus[3];
        for (int i = 1; i <= stack.getSize(); i++) {
            psfPerChannel[i - 1] = new ImagePlus(String.format("PSF channel %d", i), stack.getProcessor(i));
        }
        return psfPerChannel;
    }

    private void applyToChannel(ImageProcessor ipInput, ImagePlus psf, int iterations, float deringStrength, double deringRadius, float blendRawFactor) {
        short[] pixels = (short[]) ipInput.getPixels();
        double averagePixelValueIn = getAveragePixelValue(pixels);
        short[] outPixels = getDeconvolvedPixels(ipInput, psf, iterations);
        double averagePixelValueOut = getAveragePixelValue(outPixels);
        ImageProcessor ipMask = LswImageProcessingUtil.createDeringMaskProcessor(deringStrength, deringRadius, 4.0, ipInput);
        int maskStartX = 0;
        int maskStartY = 0;
        short[] maskPixels = null;
        if (ipMask != null) {
            ipMask = Scaler.resize(new ImagePlus("mask", ipMask), (int) (ipMask.getWidth() - (deringRadius * 2)), (int) (ipMask.getHeight() - (deringRadius * 2)), 1,
                    "depth=%s interpolation=Bicubic create".formatted(1)).getProcessor();
            maskStartX = ((ipInput.getWidth() - ipMask.getWidth()) / 2) + 2;
            maskStartY = ((ipInput.getHeight() - ipMask.getHeight()) / 2) + 2;
            maskPixels = (short[]) ipMask.getPixels();
        }
        for (int y = 0; y < ipInput.getHeight(); y++) {
            for (int x = 0; x < ipInput.getWidth(); x++) {
                int i = ipInput.getWidth() * y + x;
                double maskPixel = 0d;
                if (maskPixels != null && x >= maskStartX && x < (ipInput.getWidth() - maskStartX) && y >= maskStartY && y < (ipInput.getHeight() - maskStartY)) {
                    int maskIndex = (x - maskStartX) + ((y - maskStartY) * ipMask.getWidth());
                    maskPixel = maskIndex >= maskPixels.length ? 0d : LswImageProcessingUtil.convertToUnsignedInt(maskPixels[maskIndex]);
                }
                double appliedMaskFactor = (1d - deringStrength) + ((maskPixel / 65535d) * deringStrength);
                // correction on output is needed given that is somehow always brighter than the original value
                double newValue = (LswImageProcessingUtil.convertToUnsignedInt(outPixels[i]) * (averagePixelValueIn / averagePixelValueOut));
                double originalValue = LswImageProcessingUtil.convertToUnsignedInt(pixels[i]);
                double assignedValueAfterMask = (newValue * appliedMaskFactor) + (originalValue * (1d - appliedMaskFactor));
                double assignedValue = (1f - blendRawFactor) * assignedValueAfterMask + originalValue * blendRawFactor;
                pixels[i] = LswImageProcessingUtil.convertToShort((int) assignedValue);
            }
        }
    }

    private void applyLuminance(ImagePlus image, ImagePlus psf, WienerDeconvolutionParameters parameters) {
        ImageStack stack = image.getStack();
        ImageProcessor ipRed = stack.getProcessor(1);
        ImageProcessor ipGreen = stack.getProcessor(2);
        ImageProcessor ipBlue = stack.getProcessor(3);

        // Obtain the luminance channel
        FloatProcessor fpRed = ipRed.toFloat(1, null);
        FloatProcessor fpGreen = ipGreen.toFloat(2, null);
        FloatProcessor fpBlue = ipBlue.toFloat(3, null);
        float[] pixelsRed = (float[]) fpRed.getPixels();
        float[] pixelsGreen = (float[]) fpGreen.getPixels();
        float[] pixelsBlue = (float[]) fpBlue.getPixels();
        float[] pixelsHue = new float[pixelsRed.length];
        float[] pixelsSat = new float[pixelsRed.length];
        float[] pixelsLum = new float[pixelsRed.length];
        for (int i = 0; i < pixelsRed.length; i++) {
            float[] hsl = LswImageProcessingUtil.rgbToHsl(pixelsRed[i],
                    pixelsGreen[i],
                    pixelsBlue[i],
                    parameters.isIncludeRed(),
                    parameters.isIncludeGreen(),
                    parameters.isIncludeBlue(),
                    parameters.isIncludeColor(),
                    parameters.getMode());
            pixelsHue[i] = hsl[0];
            pixelsSat[i] = hsl[1];
            pixelsLum[i] = hsl[2];
        }
        FloatProcessor fpLum = new FloatProcessor(image.getWidth(), image.getHeight(), pixelsLum);
        ShortProcessor ipInput = new ShortProcessor(image.getWidth(), image.getHeight());
        ipInput.setPixels(1, fpLum);
        ImagePlus[] psfPerChannel = getPsfPerChannel(psf);
        applyToChannel(ipInput, psfPerChannel[1], parameters.getIterationsLuminance(), parameters.getDeringStrengthLuminance(), parameters.getDeringRadiusLuminance(), parameters.getBlendRawLuminance());
        FloatProcessor fpOut = ipInput.toFloat(3, null);
        pixelsLum = (float[])fpOut.getPixels();

        // Convert back to 16-bit RGB and update the image
        for (int i = 0; i < pixelsRed.length; i++) {
            float[] rgb = LswImageProcessingUtil.hslToRgb(pixelsHue[i], pixelsSat[i], pixelsLum[i], 0f);
            pixelsRed[i] = rgb[0];
            pixelsGreen[i] = rgb[1];
            pixelsBlue[i] = rgb[2];
        }
        ipRed.setPixels(1, fpRed);
        ipGreen.setPixels(2, fpGreen);
        ipBlue.setPixels(3, fpBlue);
    }

    private short[] getDeconvolvedPixels(ImageProcessor ipInput, ImagePlus psf, int iterations) {
        WPLOptions options =
                new WPLOptions(0, 1.0, 1.0, false, false, true, 0.01, false, false, false, 0);
        LswWPLFloatIterativeDeconvolver2D deconv = new LswWPLFloatIterativeDeconvolver2D(new ImagePlus(null, ipInput), psf, IterativeEnums.BoundaryType.ZERO, IterativeEnums.ResizingType.AUTO, iterations, options);
        ShortProcessor ipOutput = (ShortProcessor) deconv.deconvolve().getProcessor();
        return (short[]) ipOutput.getPixels();
    }

    private double getAveragePixelValue(short[] pixels) {
        double sum = 0;
        for (short pixel : pixels) {
            sum += LswImageProcessingUtil.convertToUnsignedInt(pixel);
        }
        return sum / pixels.length;
    }
}
