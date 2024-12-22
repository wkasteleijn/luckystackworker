package nl.wilcokas.luckystackworker.util;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;

public class AiryDiskGenerator {

    private static final int DEFAULT_IMAGE_SIZE = 1000;

    public static void main(String[] args) throws IOException {
        // Input parameters
        double wavelength = 600; // Light wavelength in nanometers (default: 500nm)
        double airyDiskRadius = 250; // Radius of Airy disk in pixels
        double seeingIndex = 0.0; // Atmospheric seeing index (0.0 = no distortion, 1.0 = maximum distortion)
        float diffractionIntensity = 20.0f; // Diffraction intensity (default: 20.0, min = 0, max = 1000)
        int imageSize = DEFAULT_IMAGE_SIZE; // Size of the output image (default: 100)

        // Generate the Airy disk image
        //FloatProcessor airyDiskImage = generate(wavelength, airyDiskRadius, seeingIndex, diffractionIntensity, imageSize);

        // Display the image using ImageJ
//        ImagePlus image = new ImagePlus("Synthetic Airy Disk", airyDiskImage);
        ImagePlus image = generate16BitRGB(airyDiskRadius, seeingIndex, diffractionIntensity, imageSize);
        LswFileUtil.saveImage(image, null, "C:/Users/wkast/Downloads/airy_disk.tif", false, false, false, false);
    }

    public static ImagePlus generate16BitRGB(double airyDiskRadius, double seeingIndex, float diffractionIntensity, int imageSize) {
        FloatProcessor fpRed = generate(630, airyDiskRadius, seeingIndex, diffractionIntensity, imageSize);
        FloatProcessor fpGreen = generate(532, airyDiskRadius, seeingIndex, diffractionIntensity, imageSize);
        FloatProcessor fpBlue = generate(465, airyDiskRadius, seeingIndex, diffractionIntensity, imageSize);
        short[] redPixels = new short[(int) Math.pow(imageSize, 2)];
        short[] greenPixels = new short[redPixels.length];
        short[] bluePixels = new short[redPixels.length];
        Pair<Float, Float> redMinAndMax = LswImageProcessingUtil.getMinAnMaxValues(fpRed);
        Pair<Float, Float> greenMinAndMax = LswImageProcessingUtil.getMinAnMaxValues(fpGreen);
        Pair<Float, Float> blueMinAndMax = LswImageProcessingUtil.getMinAnMaxValues(fpBlue);
        for (int i = 0; i < redPixels.length; i++) {
            redPixels[i] = LswImageProcessingUtil.convertToShort(fpRed.getf(i), redMinAndMax.getLeft(), redMinAndMax.getRight());
            greenPixels[i] = LswImageProcessingUtil.convertToShort(fpGreen.getf(i), greenMinAndMax.getLeft(), greenMinAndMax.getRight());
            bluePixels[i] = LswImageProcessingUtil.convertToShort(fpBlue.getf(i), blueMinAndMax.getLeft(), blueMinAndMax.getRight());
        }
        LswImageLayersDto layers = LswImageLayersDto.builder().layers(new short[][]{redPixels, greenPixels, bluePixels}).build();
        return LswImageProcessingUtil.create16BitRGBImage(null, layers, imageSize, imageSize, true, true, true);
    }

    private static FloatProcessor generate(double wavelength, double airyDiskRadius, double seeingIndex, float diffractionIntensity, int imageSize) {
        FloatProcessor processor = new FloatProcessor(imageSize, imageSize);

        double centerX = imageSize / 2.0;
        double centerY = imageSize / 2.0;

        // Convert wavelength from nanometers to microns for scaling (1 nm = 1e-3 microns)
        double k = 2 * Math.PI / (wavelength / 1000.0); // wavenumber in radians per pixel

        double maxIntensity = 0; // Track the maximum intensity for normalization

        // Step 1: First pass through the image to compute intensities and apply the seeing effect
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                double radius = Math.sqrt(dx * dx + dy * dy);

                // Compute intensity based on radius and adjust for the Airy disk size
                double intensity = 0.0;
                if (radius <= airyDiskRadius * 3.0) { // Extend beyond airyDiskRadius to capture rings
                    intensity = airyDiskIntensity(radius, k, airyDiskRadius);
                }

                // Update maximum intensity
                maxIntensity = Math.max(maxIntensity, intensity);

                processor.setf(x, y, (float) intensity);
            }
        }

        normalize(maxIntensity, imageSize, processor);
        increaseShadows(imageSize, processor, diffractionIntensity);

        GaussianBlur gb = new GaussianBlur();
        double radius = (seeingIndex / 2.0) * airyDiskRadius;
        gb.blurGaussian(processor, radius, radius, 0.01);

        return processor;
    }

    private static void increaseShadows(int imageSize, FloatProcessor processor, float preScaleFactor) {
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                float originalIntensity = processor.getf(x, y);
                float scaledIntensity = originalIntensity * preScaleFactor;
                float transformedIntensity = (float) Math.log(1 + scaledIntensity);
                processor.setf(x, y, transformedIntensity);
            }
        }
    }

    private static void normalize(double maxIntensity, int imageSize, FloatProcessor processor) {
        if (maxIntensity > 0) {
            for (int y = 0; y < imageSize; y++) {
                for (int x = 0; x < imageSize; x++) {
                    float normalizedIntensity = (float) (processor.getf(x, y) / maxIntensity);
                    processor.setf(x, y, normalizedIntensity);
                }
            }
        }
    }

    private static double airyDiskIntensity(double radius, double k, double airyDiskRadius) {
        if (radius == 0) {
            return 1.0;  // Central peak (intensity at the center)
        }

        double scaledRadius = radius / airyDiskRadius; // Normalize the radius to the size of the Airy disk
        double x = k * scaledRadius; // Radial distance scaled by the wavenumber

        // Bessel function of the first kind (J1), necessary for the Airy disk pattern
        double j1 = org.apache.commons.math3.special.BesselJ.value(1, x);

        // Intensity is proportional to the square of the [J1(x) / x] term
        return Math.pow(2 * j1 / x, 2);
    }
}
