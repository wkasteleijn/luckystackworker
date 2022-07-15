package nl.wilcokas.luckystackworker.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "profiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "name")
	private String name;

	@Column(name = "radius")
	private BigDecimal radius;

	@Column(name = "amount")
	private BigDecimal amount;

	@Column(name = "iterations")
	private int iterations;

	@Column(name = "level")
	private int level;

	@Column(name = "denoise")
	private BigDecimal denoise;

	@Column(name = "denoise_sigma")
	private BigDecimal denoiseSigma;

	@Column(name = "denoise_radius")
	private BigDecimal denoiseRadius;

	@Column(name = "denoise_iterations")
	private int denoiseIterations;

	@Column(name = "gamma")
	private BigDecimal gamma;

	@Column(name = "red")
	private BigDecimal red;

	@Column(name = "green")
	private BigDecimal green;

	@Column(name = "blue")
	private BigDecimal blue;

	@Column(name = "saturation")
	private BigDecimal saturation;

	@Column(name = "contrast")
	private int contrast;

	@Column(name = "brightness")
	private int brightness;

	@Column(name = "background")
	private int background;

	// Non persisted setting properties that are passed to the frontend.
	private String operation;
	private String rootFolder;
	@Transient
	private boolean isLargeImage;
}
