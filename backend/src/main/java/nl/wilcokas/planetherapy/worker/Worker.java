package nl.wilcokas.planetherapy.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.PlanetherapyContext;
import nl.wilcokas.planetherapy.constants.Constants;
import nl.wilcokas.planetherapy.service.MacroService;
import nl.wilcokas.planetherapy.util.Util;

@Slf4j
public class Worker extends Thread {

	private static final int WAIT_DELAY = 4000;
	private static final String SKIP_FOLDER_FILE = "SAS_SKIP.txt";

	private String inputFolder;
	private String outputFormat;
	private MacroService macro;

	private boolean running = true;

	public Worker(Properties properties) throws IOException {
		this.inputFolder = properties.getProperty("inputFolder");
		this.outputFormat = properties.getProperty("outputFormat");
		this.macro = new MacroService(properties);

	}

	@Override
	public void run() {
		log.info("===== AutostakkertSharpenAutomator =====");
		try {
			while (running) {
				log.info("Waiting for files to arrive from AutoStakkert...");
				Collection<Path> folders = Files.list(Paths.get(inputFolder)).collect(Collectors.toList());
				for (Path folder : folders) {
					if (Constants.KNOWN_PROFILES.contains(folder.getFileName().toString().toLowerCase())) {
						processFiles(folder);
					}
				}
				Util.pause(WAIT_DELAY);
			}
		} catch (IOException | InterruptedException e) {
			log.error("Error:", e);
		}
	}

	public void stopRunning() {
		running = false;
	}

	private void processFiles(Path folder) {
		String[] extensions = PlanetherapyContext.getWorkerProperties().getProperty("extensions").split(",");
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

}
