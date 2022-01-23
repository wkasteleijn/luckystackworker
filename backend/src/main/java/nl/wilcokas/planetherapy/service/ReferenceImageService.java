package nl.wilcokas.planetherapy.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.model.OperationEnum;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.util.Util;

@Slf4j
@Service
public class ReferenceImageService {

	private static final int STACK_POSITION_RED = 1;
	private static final int STACK_POSITION_GREEN = 2;
	private static final int STACK_POSITION_BLUE = 3;

	private ImagePlus referenceImage;
	private ImagePlus processedImage;
	private ImagePlus finalResultImage;
	private OperationEnum previousOperation;

	public void openReferenceImage(String filePath, Profile profile) {
		referenceImage = IJ.openImage(Util.getIJFileFormat(filePath));
		IJ.run(referenceImage, "32-bit", "");
		IJ.run(referenceImage, "Gamma...", "value=0.95"); // Apply initial gamma to correct imagej overexposure issue
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
	}

	public void updateProcessing(Profile profile) {
		final OperationEnum operation = profile.getOperation() == null ? null
				: OperationEnum.valueOf(profile.getOperation().toUpperCase());
		if (previousOperation == null || previousOperation != operation) {
			copyInto(referenceImage, processedImage);
			if (isSharpenOperation(operation)) {
				applyAllOperationsExcept(processedImage, profile, operation, OperationEnum.DENOISE);
			} else {
				applyAllOperationsExcept(processedImage, profile, operation);
			}
			previousOperation = operation;
		}
		copyInto(processedImage, finalResultImage);

		if (isSharpenOperation(operation)) {
			applySharpen(finalResultImage, profile);
			applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.DENOISE == operation) {
			applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.GAMMA == operation) {
			applyGamma(finalResultImage, profile);
		} else if (OperationEnum.RED == operation) {
			applyRed(finalResultImage, profile);
		} else if (OperationEnum.GREEN == operation) {
			applyGreen(finalResultImage, profile);
		} else if (OperationEnum.BLUE == operation) {
			applyBlue(finalResultImage, profile);
		}
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
	}

	private boolean isSharpenOperation(final OperationEnum operation) {
		return ((OperationEnum.AMOUNT == operation) || (OperationEnum.RADIUS == operation)
				|| (OperationEnum.ITERATIONS == operation));
	}

	private void applyAllOperationsExcept(final ImagePlus image, final Profile profile,
			final OperationEnum... operations) {
		List<OperationEnum> operationList = Arrays.asList(operations);
		if ((!operationList.contains(OperationEnum.AMOUNT)) && (!operationList.contains(OperationEnum.RADIUS))
				&& (!operationList.contains(OperationEnum.ITERATIONS))) {
			applySharpen(image, profile);
		}
		if (!operationList.contains(OperationEnum.DENOISE)) {
			applyDenoise(image, profile);
		}
		if (!operationList.contains(OperationEnum.GAMMA)) {
			applyGamma(image, profile);
		}
		if (!operationList.contains(OperationEnum.RED)) {
			applyRed(image, profile);
		}
		if (!operationList.contains(OperationEnum.GREEN)) {
			applyGreen(image, profile);
		}
		if (!operationList.contains(OperationEnum.BLUE)) {
			applyBlue(image, profile);
		}
	}

	private void applySharpen(final ImagePlus image, Profile profile) {
		int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
		if (profile.getRadius() != null && profile.getAmount() != null) {
			log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
					profile.getAmount(), iterations, image.getID());
			for (int i = 0; i < iterations; i++) {
				IJ.run(image, "Unsharp Mask...",
						String.format("radius=%s mask=%s", profile.getRadius(),
								profile.getAmount().divide(new BigDecimal("10000"))));
			}
		}
	}

	private void applyDenoise(final ImagePlus image, final Profile profile) {
		if (profile.getDenoise() != null && (profile.getDenoise().compareTo(BigDecimal.ZERO) > 0)) {
			log.info("Applying denoise with theta {} to image {}", profile.getDenoise(), image.getID());
			IJ.run(image, "ROF Denoise...",
					String.format("theta=%s", profile.getDenoise()));
		}
	}

	private void applyGamma(final ImagePlus image, final Profile profile) {
		if (profile.getGamma() != null) {
			log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
			IJ.run(image, "Gamma...", String.format("value=%s", profile.getGamma()));
		}
	}

	private void applyRed(final ImagePlus image, final Profile profile) {
		if (profile.getRed() != null && !BigDecimal.ZERO.equals(profile.getRed())) {
			log.info("Applying red reduction with value {} to image {}", profile.getRed(), image.getID());
			getImageStackProcessor(image, STACK_POSITION_RED).gamma(getGammaColorValue(profile.getRed()));
			image.updateAndDraw();
		}
	}

	private void applyGreen(final ImagePlus image, final Profile profile) {
		if (profile.getGreen() != null && !BigDecimal.ZERO.equals(profile.getGreen())) {
			log.info("Applying green reduction with value {} to image {}", profile.getGreen(), image.getID());
			getImageStackProcessor(image, STACK_POSITION_GREEN).gamma(getGammaColorValue(profile.getGreen()));
			image.updateAndDraw();
		}
	}

	private void applyBlue(final ImagePlus image, final Profile profile) {
		if (profile.getBlue() != null && !BigDecimal.ZERO.equals(profile.getBlue())) {
			log.info("Applying blue reduction with value {} to image {}", profile.getBlue(), image.getID());
			getImageStackProcessor(image, STACK_POSITION_BLUE).gamma(getGammaColorValue(profile.getBlue()));
			image.updateAndDraw();
		}
	}


	private ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
		return img.getStack().getProcessor(stackPosition);
	}

	private float getGammaColorValue(BigDecimal colorValue) {
		float value = 1.0F - (colorValue.divide(new BigDecimal("1000"))).floatValue();
		return value;
	}
}
