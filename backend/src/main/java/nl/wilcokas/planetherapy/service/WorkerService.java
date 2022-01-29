package nl.wilcokas.planetherapy.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.constants.Constants;
import nl.wilcokas.planetherapy.util.Operations;
import nl.wilcokas.planetherapy.util.Util;

@Slf4j
public class WorkerService {

	private Map<String, String> properties;

	public WorkerService(Map<String, String> properties) throws IOException {
		this.properties = properties;
	}

	public void applyProfile(final File file, final String outputFormat) {
		String profile = getProfile(file);
		log.info("Applying sharpen filter with profile '{}' to: {}", profile, file.getAbsolutePath());

		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		Operations.applyInitialSettings(imp);
		Operations.applyAllOperations(imp, properties, profile);
		IJ.save(imp, getOutputFile(file, outputFormat));
	}

	private String getProfile(File file) {
		String filename[] = file.getName().toString().split("_");
		String profile = null;
		if (filename.length > 0) {
			String profilePart = filename[0].toLowerCase();
			if (Constants.KNOWN_PROFILES.contains(profilePart)) {
				profile = profilePart.toLowerCase();
			}
		}
		return profile == null ? properties.get("default.profile").toLowerCase() : profile;
	}

	private String getOutputFile(final File file, final String outputFormat) {
		String[] filename = Util.getFilename(file);
		return filename[0] + Constants.OUTPUT_POSTFIX + "." + outputFormat;
	}
}
