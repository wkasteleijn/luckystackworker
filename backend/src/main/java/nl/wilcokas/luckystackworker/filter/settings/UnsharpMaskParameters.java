package nl.wilcokas.luckystackworker.filter.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UnsharpMaskParameters {
    private double radius;
    private float amount;
    private int iterations;
    private float clippingStrength;
    private float clippingRange;
}
