package nl.wilcokas.luckystackworker.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LswImageLayersDto {
    private short[][] layers;
    private int count;
}
