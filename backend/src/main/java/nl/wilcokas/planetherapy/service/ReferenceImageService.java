package nl.wilcokas.planetherapy.service;

import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.model.OperationEnum;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.util.Operations;
import nl.wilcokas.planetherapy.util.Util;

@Slf4j
@Service
public class ReferenceImageService {

	private ImagePlus referenceImage;
	private ImagePlus processedImage;
	private ImagePlus finalResultImage;
	private OperationEnum previousOperation;
	private String filePath;

	public void openReferenceImage(String filePath, Profile profile) {
		referenceImage = IJ.openImage(Util.getIJFileFormat(filePath));
		if (referenceImage != null) {
			Operations.applyInitialSettings(referenceImage);
			log.info("Opened reference image with id {}", referenceImage.getID());
			referenceImage.show(filePath);

			processedImage = referenceImage.duplicate();
			log.info("Opened duplicate image with id {}", processedImage.getID());
			processedImage.show();

			finalResultImage = processedImage.duplicate();
			finalResultImage.show();
			log.info("Showing 2nd duplicate image with id {}", finalResultImage.getID());
			referenceImage.getWindow().setVisible(false);
			processedImage.getWindow().setVisible(false);
			updateProcessing(profile);

			this.filePath = filePath;
		}
	}

	public void updateProcessing(Profile profile) {
		final OperationEnum operation = profile.getOperation() == null ? null
				: OperationEnum.valueOf(profile.getOperation().toUpperCase());
		if (previousOperation == null || previousOperation != operation) {
			copyInto(referenceImage, processedImage);
			if (Operations.isSharpenOperation(operation)) {
				Operations.applyAllOperationsExcept(processedImage, profile, operation, OperationEnum.DENOISE);
			} else {
				Operations.applyAllOperationsExcept(processedImage, profile, operation);
			}
			previousOperation = operation;
		}
		copyInto(processedImage, finalResultImage);

		if (Operations.isSharpenOperation(operation)) {
			Operations.applySharpen(finalResultImage, profile);
			Operations.applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.DENOISE == operation) {
			Operations.applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.GAMMA == operation) {
			Operations.applyGamma(finalResultImage, profile);
		} else if (OperationEnum.RED == operation) {
			Operations.applyRed(finalResultImage, profile);
		} else if (OperationEnum.GREEN == operation) {
			Operations.applyGreen(finalResultImage, profile);
		} else if (OperationEnum.BLUE == operation) {
			Operations.applyBlue(finalResultImage, profile);
		}
	}

	public void saveReferenceImage(String path) {
		String dir = Util.getFileDirectory(filePath);
		log.info("Saving image to folder {}", dir);
		IJ.save(finalResultImage, path);
		log.info("Saved file to {}", path);
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
	}

}
