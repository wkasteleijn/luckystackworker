package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class RotationFilter implements LSWFilter {
    @Override
    public boolean apply(ImagePlus image, Profile profile, boolean isMono) throws Exception {
        if (isApplied(profile, image)) {
            ImageStack stack = image.getStack();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[3];
                futures[0] = CompletableFuture.runAsync(() -> stack.getProcessor(1).rotate(profile.getRotationAngle()), executor);
                futures[1] = CompletableFuture.runAsync(() -> stack.getProcessor(2).rotate(profile.getRotationAngle()), executor);
                futures[2] = CompletableFuture.runAsync(() -> stack.getProcessor(3).rotate(profile.getRotationAngle()), executor);
                CompletableFuture.allOf(futures).get();
            }
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
