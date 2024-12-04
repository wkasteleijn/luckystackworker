package nl.wilcokas.luckystackworker.filter.settings;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ROFParameters {
    private float tethaRed;
    private int iterationsRed;
    private float gRed; // = 1;
    private float dtRed; // = 0.25f;

    private float tethaGreen;
    private int iterationsGreen;
    private float gGreen;
    private float dtGreen;

    private float tethaBlue;
    private int iterationsBlue;
    private float gBlue;
    private float dtBlue;
}
