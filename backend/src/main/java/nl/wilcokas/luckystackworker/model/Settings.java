package nl.wilcokas.luckystackworker.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "settings")
@NoArgsConstructor
@AllArgsConstructor
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
}
