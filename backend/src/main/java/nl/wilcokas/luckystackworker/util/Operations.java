package nl.wilcokas.luckystackworker.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
public final class Operations {
	private static final int STACK_POSITION_RED = 1;
	private static final int STACK_POSITION_GREEN = 2;
	private static final int STACK_POSITION_BLUE = 3;

	public static void applyInitialSettings(ImagePlus image) {
		IJ.run(image, "32-bit", "");
		IJ.run(image, "Gamma...", "value=0.95"); // Apply initial gamma to correct imagej overexposure issue
	}

	public static boolean isSharpenOperation(final OperationEnum operation) {
		return ((OperationEnum.AMOUNT == operation) || (OperationEnum.RADIUS == operation)
				|| (OperationEnum.ITERATIONS == operation));
	}

	public static void applyAllOperationsExcept(final ImagePlus image, final Profile profile,
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

	public static void applyAllOperations(final ImagePlus image, final Map<String, String> profileSettings,
			String profileName) {
		final Profile profile = Util.toProfile(profileSettings, profileName);
		applySharpen(image, profile);
		applyDenoise(image, profile);
		applyGamma(image, profile);
		applyRed(image, profile);
		applyGreen(image, profile);
		applyBlue(image, profile);
	}

	public static void applySharpen(final ImagePlus image, Profile profile) {
		int iterations = profile.getIterations() == 0 ? 1 : profile.getIterations();
		if (profile.getRadius() != null && profile.getAmount() != null) {
			log.info("Applying sharpen with radius {}, amount {}, iterations {} to image {}", profile.getRadius(),
					profile.getAmount(), iterations, image.getID());
			for (int i = 0; i < iterations; i++) {
				IJ.run(image, "Unsharp Mask...", String.format("radius=%s mask=%s", profile.getRadius(),
						profile.getAmount().divide(new BigDecimal("10000"))));
			}
		}
	}

	public static void applyDenoise(final ImagePlus image, final Profile profile) {
		if (profile.getDenoise() != null && (profile.getDenoise().compareTo(BigDecimal.ZERO) > 0)) {
			log.info("Applying denoise with theta {} to image {}", profile.getDenoise(), image.getID());
			IJ.run(image, "ROF Denoise...", String.format("theta=%s", profile.getDenoise()));
		}
	}

	public static void applyGamma(final ImagePlus image, final Profile profile) {
		if (profile.getGamma() != null && (profile.getGamma().compareTo(BigDecimal.ONE) != 0)) {
			log.info("Applying gamma correction with value {} to image {}", profile.getGamma(), image.getID());
			IJ.run(image, "Gamma...", String.format("value=%s", profile.getGamma()));
		}
	}

	public static void applyRed(final ImagePlus image, final Profile profile) {
		if (profile.getRed() != null && (profile.getRed().compareTo(BigDecimal.ZERO) > 0)) {
			if (validateRGBStack(image)) {
				log.info("Applying red reduction with value {} to image {}", profile.getRed(), image.getID());
				getImageStackProcessor(image, STACK_POSITION_RED).gamma(getGammaColorValue(profile.getRed()));
				image.updateAndDraw();
			} else {
				log.warn("Attemping to apply red reduction to a non RGB image {}", image.getFileInfo());
			}
		}
	}

	public static void applyGreen(final ImagePlus image, final Profile profile) {
		if (profile.getGreen() != null && (profile.getGreen().compareTo(BigDecimal.ZERO) > 0)) {
			if (validateRGBStack(image)) {
				log.info("Applying green reduction with value {} to image {}", profile.getGreen(), image.getID());
				getImageStackProcessor(image, STACK_POSITION_GREEN).gamma(getGammaColorValue(profile.getGreen()));
				image.updateAndDraw();
			} else {
				log.warn("Attemping to apply green reduction to a non RGB image {}", image.getFileInfo());
			}
		}
	}

	public static void applyBlue(final ImagePlus image, final Profile profile) {
		if (profile.getBlue() != null && (profile.getBlue().compareTo(BigDecimal.ZERO) > 0)) {
			if (validateRGBStack(image)) {
				log.info("Applying blue reduction with value {} to image {}", profile.getBlue(), image.getID());
				getImageStackProcessor(image, STACK_POSITION_BLUE).gamma(getGammaColorValue(profile.getBlue()));
				image.updateAndDraw();
			} else {
				log.warn("Attemping to apply blue reduction to a non RGB image {}", image.getFileInfo());
			}
		}
	}

	private static ImageProcessor getImageStackProcessor(final ImagePlus img, final int stackPosition) {
		return img.getStack().getProcessor(stackPosition);
	}

	private static float getGammaColorValue(BigDecimal colorValue) {
		float value = 1.0F - (colorValue.divide(new BigDecimal("1000"))).floatValue();
		return value;
	}

	private static boolean validateRGBStack(ImagePlus image) {
		return image.getStack().size() == 3;
	}

}
