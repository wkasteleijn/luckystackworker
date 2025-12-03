package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.wilcokas.luckystackworker.model.PSFType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PSFDTO {
    private double airyDiskRadius;
    private double seeingIndex;
    private double diffractionIntensity;
    private PSFType type;
}
