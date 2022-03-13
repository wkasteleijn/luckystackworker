package nl.wilcokas.luckystackworker.util;

import java.awt.image.ColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.util.ReflectionUtils;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileSaver;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;

@Slf4j
public class Util {
	public static boolean fileExists(String path) {
		return Files.exists(Paths.get(path));
	}

	public static String[] getFilename(File file) {
		return getFilename(file.getAbsolutePath());
	}

	public static String[] getFilename(String path) {
		return path.split("\\.");
	}

	@SuppressWarnings("static-access")
	public static void pause(long waitDelay) throws InterruptedException {
		Thread.currentThread().sleep(waitDelay);
	}

	public static String getIJFileFormat(String path) {
		return path.replaceAll("\\\\", "/");
	}

	public static String deriveProfileFromImageName(String path) {
		String name = getImageName(getIJFileFormat(path)).toLowerCase();
		if (name.startsWith("mer")) {
			return "mer";
		} else if (name.startsWith("ven")) {
			return "ven";
		} else if (name.startsWith("moon")) {
			return "moon";
		} else if (name.startsWith("mars")) {
			return "mars";
		} else if (name.startsWith("jup")) {
			return "jup";
		} else if (name.startsWith("sat")) {
			return "sat";
		} else if (name.startsWith("uranus")) {
			return "uranus";
		} else if (name.startsWith("neptune")) {
			return "neptune";
		} else if (name.startsWith("sun")) {
			return "sun";
		}
		return null;
	}

	public static String getFileDirectory(String path) {
		String ijFormatPath = getIJFileFormat(path);
		return ijFormatPath.substring(0, ijFormatPath.lastIndexOf("/"));
	}

	public static String getImageName(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}

	public static Profile toProfile(Map<String, String> props, final String profileName) {
		return Profile.builder().amount(new BigDecimal(getSetting(props, "amount", profileName))) //
				.radius(new BigDecimal(getSetting(props, "radius", profileName))) //
				.iterations(Integer.valueOf(getSetting(props, "iterations", profileName))) //
				.denoise(new BigDecimal(getSetting(props, "denoise", profileName))) //
				.gamma(new BigDecimal(getSetting(props, "gamma", profileName))) //
				.red(new BigDecimal(getSetting(props, "red", profileName))) //
				.green(new BigDecimal(getSetting(props, "green", profileName))) //
				.blue(new BigDecimal(getSetting(props, "blue", profileName))).name(profileName).build();
	}

	public static String readFromInputStream(InputStream inputStream) {
		try {
			StringBuilder resultStringBuilder = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
				String line;
				while ((line = br.readLine()) != null) {
					resultStringBuilder.append(line).append("\n");
				}
			}
			return resultStringBuilder.toString();
		} catch (IOException e) {
			log.error("Error reading from inputStream: ", e);
		}
		return null;
	}

	public static void deleteFile(String path) throws IOException {
		Files.delete(Paths.get(path));
	}

	public static void saveImage(ImagePlus image, String path, boolean isPngRgbStack) throws IOException {
		if (isPngRgbStack) {
			image.setActiveChannels("111");
			image.setC(1);
			image.setZ(1);
		}
		FileSaver saver = new FileSaver(image);
		if (isPngRgbStack) {
			hackIncorrectPngFileInfo(saver);
		}
		saver.saveAsTiff(path);
	}

	public static ImagePlus fixNonTiffOpeningSettings(ImagePlus image) {
		log.info("Applying workaround for correctly opening PNG RGB stack");
		ImagePlus result = new CompositeImage(image, IJ.COMPOSITE);
		result.getStack().setSliceLabel("red", 1);
		result.getStack().setSliceLabel("green", 2);
		result.getStack().setSliceLabel("blue", 3);
		result.getStack().setColorModel(ColorModel.getRGBdefault());
		result.setActiveChannels("111");
		result.setC(1);
		result.setZ(1);
		result.setDisplayMode(IJ.COMPOSITE);
		result.setOpenAsHyperStack(true);
		result.getFileInfo().fileType = FileInfo.RGB48;
		return result;
	}

	public static int getMaxHistogramPercentage(ImagePlus image) {
		int[] histogram = image.getProcessor().getHistogram();
		int maxVal = 0;
		for (int i = histogram.length - 1; i >= 0; i--) {
			if (histogram[i] > 0) {
				maxVal = i;
				break;
			}
		}
		return (maxVal * 100) / 65536;
	}

	public static boolean isPngRgbStack(ImagePlus image, String filePath) {
		return filePath.toLowerCase().endsWith(".png") && image.isStack() && image.getStack().getSize() > 1;
	}

	private static String getSetting(Map<String, String> props, String setting, String name) {
		return props.get(name + "." + setting);
	}

	private static void hackIncorrectPngFileInfo(FileSaver saver) {
		Field field = ReflectionUtils.findField(FileSaver.class, "fi");
		ReflectionUtils.makeAccessible(field);
		FileInfo fileInfo = (FileInfo) ReflectionUtils.getField(field, saver);
		Field fileType = ReflectionUtils.findField(FileInfo.class, "fileType");
		ReflectionUtils.makeAccessible(fileType);
		ReflectionUtils.setField(fileType, fileInfo, FileInfo.RGB48);
	}

}
