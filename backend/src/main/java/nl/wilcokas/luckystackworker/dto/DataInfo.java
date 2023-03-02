package nl.wilcokas.luckystackworker.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataInfo {
    private String version;
    private LocalDate installationDate;
}
