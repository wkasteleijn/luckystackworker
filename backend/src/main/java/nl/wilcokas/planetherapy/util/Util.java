package nl.wilcokas.planetherapy.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Util {
	public static boolean fileExists(String path) {
		return Files.exists(Paths.get(path));
	}

	public static String[] getFilename(File file) {
		return file.getAbsolutePath().split("\\.");
	}

	public static void logInfo(String message) {
		System.out.println("[INFO]: " + message);
	}

	public static void logError(Exception e) {
		System.out.println("[ERROR]: " + e.getMessage());
	}

	@SuppressWarnings("static-access")
	public static void pause(long waitDelay) throws InterruptedException {
		Thread.currentThread().sleep(waitDelay);
	}

	public static String getIJFileFormat(String path) {
		return path.replaceAll("\\\\", "/");
	}

	public static String getTempFile(String path) {
		String[] pathSep = path.split("\\.");
		return pathSep[0] + "_tmp.png";
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

	private static String getImageName(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}

}
