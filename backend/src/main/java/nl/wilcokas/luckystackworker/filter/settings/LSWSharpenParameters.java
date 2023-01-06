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

    // Applies to non individual+luminace & RGB mode
    private UnsharpMaskParameters unsharpMaskParameters;

    // Applies to non individual+luminance mode
    private boolean includeRed;
    private boolean includeGreen;
    private boolean includeBlue;

    private float saturation;

    // Applies to individual+luminance mode
    private UnsharpMaskParameters unsharpMaskParametersRed;
    private UnsharpMaskParameters unsharpMaskParametersGreen;
    private UnsharpMaskParameters unsharpMaskParametersBlue;

}
