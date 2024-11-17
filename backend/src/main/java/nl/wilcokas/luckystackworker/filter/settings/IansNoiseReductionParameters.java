package nl.wilcokas.luckystackworker.filter.settings;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IansNoiseReductionParameters {
    private BigDecimal fine;
    private BigDecimal medium;
    private BigDecimal large;
    private BigDecimal recovery;
    private int iterations;
}
