package nl.wilcokas.luckystackworker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import nl.wilcokas.luckystackworker.model.Profile;

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

	public static String readFromInputStream(InputStream inputStream) throws IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

	private static String getSetting(Map<String, String> props, String setting, String name) {
		return props.get(name + "." + setting);
	}

}
