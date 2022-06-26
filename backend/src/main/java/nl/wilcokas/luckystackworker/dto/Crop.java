package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Crop {
	private int x;
	private int y;
	private int width;
	private int height;
}
