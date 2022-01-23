package nl.wilcokas.planetherapy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import nl.wilcokas.planetherapy.constants.Constants;
import nl.wilcokas.planetherapy.service.MacroService;
import nl.wilcokas.planetherapy.util.Util;

public class Worker {

	private static final String PROPERTIES_FILENAME = "ass.properties";
	private static final int WAIT_DELAY = 4000;
	private static final String SKIP_FOLDER_FILE = "SAS_SKIP.txt";

	private String inputFolder;
	private String outputFormat;
	private static Properties properties = new Properties();
	private static long propertiesLastmodified;
	private MacroService macro;

	public static void main(String[] args) {

		loadProperties();

		try {
			Worker autostakkertSharpenAutomator = new Worker(properties);
			autostakkertSharpenAutomator.run();
		} catch (IOException e) {
			Util.logError(e);
		} catch (InterruptedException e) {
			Util.logInfo("I was stopped");
		}
	}

	Worker(Properties properties) throws IOException {
		this.inputFolder = properties.getProperty("inputFolder");
		this.outputFormat = properties.getProperty("outputFormat");
		this.macro = new MacroService(properties);

	}

	private void run() throws IOException, InterruptedException {
		Util.logInfo("===== AutostakkertSharpenAutomator =====");
		boolean running = true;
		while (running) {
			Util.logInfo("Waiting for files to arrive from AutoStakkert...");
			reloadPropertiesIfNeeded();
			Collection<Path> folders = Files.list(Paths.get(inputFolder)).collect(Collectors.toList());
			for (Path folder : folders) {
				if (Constants.KNOWN_PROFILES.contains(folder.getFileName().toString().toLowerCase())) {
					processFiles(folder);
				}
			}
			Util.pause(WAIT_DELAY);
		}
	}

	private void processFiles(Path folder) {
		String[] extensions = properties.getProperty("extensions").split(",");
		Collection<File> files = FileUtils.listFiles(folder.toFile(), extensions, true);
		for (File file : files) {
			if (!skipFolder(file)) {
				String[] filename = Util.getFilename(file);
				String name = filename[0];
				String extension = filename[filename.length - 1];
				if (!name.contains(Constants.CONV_MARKER) && name.contains(Constants.AS_MARKER)
						&& !name.endsWith(Constants.OUTPUT_POSTFIX) && Arrays.asList(extensions).contains(extension)
						&& !Util.fileExists(name + Constants.OUTPUT_POSTFIX + "." + outputFormat)) {
					macro.runMacro(file);
				}
			}
		}
	}

	private boolean skipFolder(File file) {
		File folder = file.getParentFile();
		Collection<File> files = FileUtils.listFiles(folder, null, true);
		return files.stream().anyMatch(f -> SKIP_FOLDER_FILE.equals(f.getName()));
	}

	private void reloadPropertiesIfNeeded() throws IOException {
		File file = FileUtils.getFile(getUserDirectory() + "/" + PROPERTIES_FILENAME);
		if (file.lastModified() > propertiesLastmodified) {
			Util.logInfo("Reloading properties");
			loadProperties();
			this.macro = new MacroService(properties);
		}
	}

	private static String getUserDirectory() {
		return System.getProperty("user.dir");
	}

	private static void loadProperties() {
		try {
			properties.load(new FileInputStream(getUserDirectory() + "/" + PROPERTIES_FILENAME));
			propertiesLastmodified = Instant.now().getEpochSecond() * 1000L;
			Util.logInfo(properties.toString());
		} catch (IOException e) {
			Util.logError(e);
		}
	}
}
