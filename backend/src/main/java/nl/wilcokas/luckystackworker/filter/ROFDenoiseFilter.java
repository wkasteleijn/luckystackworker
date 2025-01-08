package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/**
 * This denoising method is based on total-variation, originally proposed by
 * Rudin, Osher and Fatemi. In this particular case fixed point iteration is
 * utilized.
 * <p>
 * For the included image, a fairly good result is obtained by using a theta
 * value around 12-16. A possible addition would be to analyze the residual with
 * an entropy function and add back areas that have a lower entropy, i.e. there
 * are some correlation between the surrounding pixels.
 * <p>
 * Based on the
 * <p>
 * <a href=
 * "http://www.mathworks.com/matlabcentral/fileexchange/22410-rof-denoising-algorithm">
 * Matlab code</a>
 * <p>
 * Originally written By Philippe Magiera and Carl Londahl.
 * Adapted by Wilco Kasteleijn.
 */
@Slf4j
@Component
public class ROFDenoiseFilter {

    public boolean apply(ImagePlus image, Profile profile) {
        log.info("Applying ROF denoising to image {} with theta {}, thetaGreen {}, thetaBlue {}, iterations {}, iterationsGreen {}, iterationsBlue {}",
                image.getID(), profile.getRofTheta(), profile.getRofThetaGreen(), profile.getRofThetaBlue(), profile.getRofIterations(), profile.getRofIterationsGreen(), profile.getRofIterationsBlue());
        ImageStack stack = image.getStack();
        final float g = 1;
        final float dt = 0.25f;

        // Run every stack in a seperate thread to increase performance.
        Executor executor = LswUtil.getParallelExecutor();
        executor.execute(() -> applyToChannel(stack, profile.getRofTheta() * 10, g, dt, profile.getRofIterations(), 1));
        executor.execute(() -> applyToChannel(stack, profile.getRofThetaGreen() * 10, g, dt, profile.getRofIterationsGreen(), 2));
        executor.execute(() -> applyToChannel(stack, profile.getRofThetaBlue() * 10, g, dt, profile.getRofIterationsBlue(), 3));
        LswUtil.stopAndAwaitParallelExecutor(executor);
        return true;
    }

    private void applyToChannel(ImageStack stack, final float theta, final float g, final float dt, final int iterations, final int channel) {
        ImageProcessor ip = stack.getProcessor(channel);
        FloatProcessor fp = ip.toFloat(channel, null);

        final int w = fp.getWidth();
        final int h = fp.getHeight();
        final float[] pixels = (float[]) fp.getPixels();

        final float[] u = new float[w * h];
        final float[] p = new float[w * h * 2];
        final float[] d = new float[w * h * 2];
        final float[] du = new float[w * h * 2];
        final float[] div_p = new float[w * h];

        for (int iteration = 0; iteration < iterations; iteration++) {
            for (int i = 0; i < w; i++) {
                for (int j = 1; j < h - 1; j++)
                    div_p[i + w * j] = p[i + w * j] - p[i + w * (j - 1)];
                // Handle boundaries
                div_p[i] = p[i];
                div_p[i + w * (h - 1)] = -p[i + w * (h - 1)];
            }

            for (int j = 0; j < h; j++) {
                for (int i = 1; i < w - 1; i++)
                    div_p[i + w * j] += p[i + w * (j + h)] - p[i - 1 + w * (j + h)];
                // Handle boundaries
                div_p[w * j] = p[w * (j + h)];
                div_p[w - 1 + w * j] = -p[w - 1 + w * (j + h)];
            }

            // Update u
            for (int j = 0; j < h; j++)
                for (int i = 0; i < w; i++)
                    u[i + w * j] = pixels[i + w * j] - theta * div_p[i + w * j];

            // Calculate forward derivatives
            for (int j = 0; j < h; j++)
                for (int i = 0; i < w; i++) {
                    if (i < w - 1)
                        du[i + w * (j + h)] = u[i + 1 + w * j] - u[i + w * j];
                    if (j < h - 1)
                        du[i + w * j] = u[i + w * (j + 1)] - u[i + w * j];
                }

            // Iterate
            for (int j = 0; j < h; j++)
                for (int i = 0; i < w; i++) {
                    final float du1 = du[i + w * j], du2 = du[i + w * (j + h)];
                    d[i + w * j] = 1 + dt / theta / g * Math.abs((float) Math.sqrt(du1 * du1 + du2 * du2));
                    d[i + w * (j + h)] = 1 + dt / theta / g * Math.abs((float) Math.sqrt(du1 * du1 + du2 * du2));
                    p[i + w * j] = (p[i + w * j] - dt / theta * du[i + w * j]) / d[i + w * j];
                    p[i + w * (j + h)] = (p[i + w * (j + h)] - dt / theta * du[i + w * (j + h)]) / d[i + w * (j + h)];
                }
        }
        System.arraycopy(u, 0, pixels, 0, w * h);
        ip.setPixels(channel, fp);
    }

}
