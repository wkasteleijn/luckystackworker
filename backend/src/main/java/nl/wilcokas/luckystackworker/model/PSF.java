package nl.wilcokas.luckystackworker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PSF {
  private double airyDiskRadius;
  private double seeingIndex;
  private double diffractionIntensity;
  private PSFType type;
}
