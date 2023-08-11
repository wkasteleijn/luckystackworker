package nl.wilcokas.luckystackworker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "settings")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "rootFolder")
    private String rootFolder;

    @Column(name = "extensions")
    private String extensions;

    @Column(name = "outputFormat")
    private String outputFormat;

    @Column(name = "defaultProfile")
    private String defaultProfile;

    @Column(name = "latestKnownVersion")
    private String latestKnownVersion;

    @Column(name = "latestKnownVersionChecked")
    private LocalDateTime latestKnownVersionChecked;
}
