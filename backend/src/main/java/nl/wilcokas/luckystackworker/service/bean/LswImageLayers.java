package nl.wilcokas.luckystackworker.service.bean;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LswImageLayers {
  private int width;
  private int height;
  private short[][] layers;
}
