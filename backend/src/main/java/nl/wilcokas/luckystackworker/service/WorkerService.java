package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.util.Operations;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class WorkerService {

	private Map<String, String> properties;

	public WorkerService(Map<String, String> properties) throws IOException {
		this.properties = properties;
	}

	public boolean processFile(final File file, final String outputFormat) {
		final String filePath = file.getAbsolutePath();
		Optional<String> profileOpt = getProfile(file);
		if (!profileOpt.isPresent()) {
			log.info("Could not determine a profile for file {}", filePath);
			return false;
		}
		String profile = profileOpt.get();
		if (profile.equals(LuckyStackWorkerContext.getActiveProfile())) {
			try {
				final String filename = Util.getImageName(Util.getIJFileFormat(filePath));
				log.info("Applying profile '{}' to: {}", profile, filename);
				LuckyStackWorkerContext.statusUpdate("Processing : " + filename);
				ImagePlus imp = IJ.openImage(filePath);
				Operations.applyInitialSettings(imp);
				Operations.applyAllOperations(imp, properties, profile);
				IJ.save(imp, getOutputFile(file, outputFormat));
				return true;
			} catch (Exception e) {
				log.error("Error processing file: ", e);
			}
		}
		return false;
	}

	private Optional<String> getProfile(File file) {
		String filename[] = file.getName().toString().split("_");
		Optional<String> profile = Optional.empty();
		if (filename.length > 0) {
			String profilePart = filename[0].toLowerCase();
			if (Constants.KNOWN_PROFILES.contains(profilePart)) {
				profile = Optional.ofNullable(profilePart.toLowerCase());
			}
		}
		return profile;
	}

	private String getOutputFile(final File file, final String outputFormat) {
		String[] filename = Util.getFilename(file);
		return filename[0] + Constants.OUTPUT_POSTFIX + "." + outputFormat;
	}
}
