package nl.wilcokas.luckystackworker.filter;

import ij.ImagePlus;
import nl.wilcokas.luckystackworker.model.Profile;

import java.io.IOException;

public interface LSWFilter {
    void apply(ImagePlus image, Profile profile, boolean isMono) throws IOException;
    boolean isSlow();
}
