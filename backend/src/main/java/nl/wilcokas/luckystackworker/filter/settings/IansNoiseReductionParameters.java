package nl.wilcokas.luckystackworker.filter.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class IansNoiseReductionParameters {
    private int fine;
    private int medium;
    private int large;
    private boolean recover;
}
