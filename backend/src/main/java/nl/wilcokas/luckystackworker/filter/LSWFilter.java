package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import nl.wilcokas.luckystackworker.model.Profile;

import java.io.IOException;

public interface LSWFilter {
    boolean apply(ImagePlus image, Profile profile, boolean isMono) throws Exception;
    boolean isSlow();
    boolean isApplied(Profile profile, ImagePlus image); // For testing purposes (e.g., to check if a filter is applied based on a specific profile property, like a denoise algorithm setting or saturation value. Useful)
}
