package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Executor;

@Component
public class RotationFilter implements LSWFilter {
    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws IOException {
        if (isApplied(profile, image)) {
            ImageStack stack = image.getStack();
            Executor executor = LswUtil.getParallelExecutor();
            executor.execute(() -> stack.getProcessor(1).rotate(profile.getRotationAngle()));
            executor.execute(() -> stack.getProcessor(2).rotate(profile.getRotationAngle()));
            executor.execute(() -> stack.getProcessor(3).rotate(profile.getRotationAngle()));
            LswUtil.stopAndAwaitParallelExecutor(executor);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSlow() {
        return false;
    }

    @Override
    public boolean isApplied(Profile profile, ImagePlus image) {
        return profile.getRotationAngle() != 0.0;
    }
}
