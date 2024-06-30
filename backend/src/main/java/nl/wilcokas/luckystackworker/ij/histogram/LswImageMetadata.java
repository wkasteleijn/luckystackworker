package nl.wilcokas.luckystackworker.ij.histogram;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LswImageMetadata {
    private int red;
    private int green;
    private int blue;
    private int luminance;
    private String filePath;
    private String name;
    private int width;
    private int height;
    private LocalDateTime time;
}
