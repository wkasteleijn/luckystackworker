package nl.wilcokas.luckystackworker;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import nl.wilcokas.luckystackworker.util.LSWFileSaver;

public class AiryDiskGenerator {

    private static final int IMAGE_SIZE = 256;

    public static void main(String[] args) {
        // Input parameters
        double wavelength = 600; // Light wavelength in nanometers (default: 500nm)
        double airyDiskRadius = 200; // Radius of Airy disk in pixels
        double seeingIndex = 0.0; // Atmospheric seeing index (0.0 = no distortion, 1.0 = maximum distortion)

        // Generate the Airy disk image
        FloatProcessor airyDiskImage = generateAiryDiskImage(wavelength, airyDiskRadius, seeingIndex);

        // Display the image using ImageJ
        ImagePlus imagePlus = new ImagePlus("Synthetic Airy Disk", airyDiskImage);
        imagePlus.show();
        new LSWFileSaver(imagePlus).saveAsPng("C:\\Users\\wkast\\Downloads\\airy_disk.png");
    }

    private static FloatProcessor generateAiryDiskImage(double wavelength, double airyDiskRadius, double seeingIndex) {
        int imageSize = IMAGE_SIZE;
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

                // Apply atmospheric seeing effect
                intensity = applySeeingEffect(intensity, seeingIndex, radius);

                // Update maximum intensity
                maxIntensity = Math.max(maxIntensity, intensity);

                processor.setf(x, y, (float) intensity);
            }
        }

        // Step 2: Normalize the intensities to the range [0, 1]
        if (maxIntensity > 0) {
            for (int y = 0; y < imageSize; y++) {
                for (int x = 0; x < imageSize; x++) {
                    float normalizedIntensity = (float) (processor.getf(x, y) / maxIntensity);
                    processor.setf(x, y, normalizedIntensity);
                }
            }
        }
        float preScaleFactor = 20.0f;  // Pre-scaling factor to increase intensity range before log transform
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                float originalIntensity = processor.getf(x, y);

                // Pre-scale the intensities (this increases the range of values for the log transformation)
                float scaledIntensity = originalIntensity * preScaleFactor;

                // Apply log transformation to bring out darker areas (diffraction rings)
                float transformedIntensity = (float) Math.log(1 + scaledIntensity);

                // Set the transformed intensity back to the processor
                processor.setf(x, y, transformedIntensity);
            }
        }
        return processor;
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

    private static double applySeeingEffect(double intensity, double seeingIndex, double radius) {
        if (seeingIndex == 0.0) return intensity; // No distortion if seeing index is 0

        // To simulate the seeing effect, we'll make the intensity drop off more slowly.
        // We'll use a Gaussian "spread" that increases with the seeing index.
        double spreadFactor = seeingIndex * 0.5; // The factor by which the disk will spread
        double blurFactor = Math.exp(-Math.pow(radius / (spreadFactor + 0.1), 2)); // Use a small offset to avoid division by zero

        // This will spread the Airy disk and make it appear larger while maintaining intensity.
        return intensity * blurFactor;
    }
}
