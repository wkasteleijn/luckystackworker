package nl.wilcokas.luckystackworker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Crop {
	private int x;
	private int y;
	private int width;
	private int height;
}
