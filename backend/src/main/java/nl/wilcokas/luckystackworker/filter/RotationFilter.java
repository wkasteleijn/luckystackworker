package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import ij.ImageStack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import nl.wilcokas.luckystackworker.exceptions.FilterException;
import nl.wilcokas.luckystackworker.model.Profile;
import org.springframework.stereotype.Component;

@Component
public class RotationFilter implements LSWFilter {
  @Override
  public boolean apply(ImagePlus image, Profile profile, boolean isMono) {
    if (isApplied(profile, image)) {
      ImageStack stack = image.getStack();
      try {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
          CompletableFuture<?>[] futures = new CompletableFuture[3];
          futures[0] =
              CompletableFuture.runAsync(
                  () -> stack.getProcessor(1).rotate(profile.getRotationAngle()), executor);
          futures[1] =
              CompletableFuture.runAsync(
                  () -> stack.getProcessor(2).rotate(profile.getRotationAngle()), executor);
          futures[2] =
              CompletableFuture.runAsync(
                  () -> stack.getProcessor(3).rotate(profile.getRotationAngle()), executor);
          CompletableFuture.allOf(futures).get();
        }
      } catch (InterruptedException | ExecutionException e) { // NOSONAR
        throw new FilterException(e.getMessage());
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
