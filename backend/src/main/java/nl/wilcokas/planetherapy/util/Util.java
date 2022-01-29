package nl.wilcokas.planetherapy.util;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import nl.wilcokas.planetherapy.model.Profile;

public class Util {
	public static boolean fileExists(String path) {
		return Files.exists(Paths.get(path));
	}

	public static String[] getFilename(File file) {
		return file.getAbsolutePath().split("\\.");
	}

	@SuppressWarnings("static-access")
	public static void pause(long waitDelay) throws InterruptedException {
		Thread.currentThread().sleep(waitDelay);
	}

	public static String getIJFileFormat(String path) {
		return path.replaceAll("\\\\", "/");
	}

	public static String getProcessedFileName(String path) {
		String[] pathSep = getImageName(path).split("\\.");
		return pathSep[0] + "_planetherapy";
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

	private static String getSetting(Map<String, String> props, String setting, String name) {
		return props.get(name + "." + setting);
	}

}
