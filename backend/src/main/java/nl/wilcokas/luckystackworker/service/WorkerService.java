package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import ij.ImagePlus;
import ij.io.Opener;
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

	public boolean processFile(final File file) {
		String filePath = file.getAbsolutePath();
		Optional<String> profileOpt = Optional.ofNullable(Util.deriveProfileFromImageName(file.getAbsolutePath()));
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

				ImagePlus imp = new Opener().openImage(filePath);
				if (Util.isPngRgbStack(imp, filePath)) {
					imp = Util.fixNonTiffOpeningSettings(imp);
				}
				Operations.correctExposure(imp);
				ImagePlus duplicatedImage = Operations.applyAllOperations(imp, properties, profile);
				if (duplicatedImage != null) {
					imp = duplicatedImage;
				}
				boolean isCropped = setCrop(imp);
				if (isCropped) {
					imp = imp.crop();
				}
				Util.saveImage(imp, getOutputFile(file), Util.isPngRgbStack(imp, filePath), isCropped);
				return true;
			} catch (Exception e) {
				log.error("Error processing file: ", e);
			}
		}
		return false;
	}

	private String getOutputFile(final File file) {
		String[] filename = Util.getFilename(file);
		return filename[0] + Constants.OUTPUT_POSTFIX + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
	}

	private boolean setCrop(ImagePlus imp) {
		Map<String, String> cropProps = LuckyStackWorkerContext.getWorkerProperties();
		if (cropProps.get("crop_x") != null) {
			int x = Integer.parseInt(cropProps.get("crop_x"));
			int y = Integer.parseInt(cropProps.get("crop_x"));
			int width = Integer.parseInt(cropProps.get("crop_x"));
			int height = Integer.parseInt(cropProps.get("crop_x"));
			imp.setRoi(x, y, width, height);
			return true;
		}
		return false;
	}
}
