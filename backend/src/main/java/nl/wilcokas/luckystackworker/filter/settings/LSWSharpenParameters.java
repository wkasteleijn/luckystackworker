package nl.wilcokas.luckystackworker.filter.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LSWSharpenParameters {

    private LSWSharpenMode mode;

    private boolean individual;

    // Applies to non individual+luminance & RGB mode
    private UnsharpMaskParameters unsharpMaskParameters;

    // Applies to non individual+luminance mode
    private boolean includeRed;
    private boolean includeGreen;
    private boolean includeBlue;
    private boolean includeColor;

    private float saturation;

}
