package nl.wilcokas.luckystackworker.ij.histogram;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import nl.wilcokas.luckystackworker.model.ChannelEnum;

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
    private int originalWidth;
    private int originalHeight;
    private int currentWidth;
    private int currentHeight;
    private LocalDateTime time;
    private int cropWidth;
    private int cropHeight;
    private int zoomFactor;
    private ChannelEnum channel;
    private double angle;
}
