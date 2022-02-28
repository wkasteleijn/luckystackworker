package nl.wilcokas.luckystackworker.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.service.WorkerService;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class Worker extends Thread {

	private static final int WAIT_DELAY = 4000;

	private Map<String, String> properties;
	private WorkerService workerService;

	private boolean running = true;

	public Worker(Map<String, String> properties) throws IOException {
		this.properties = properties;
		this.workerService = new WorkerService(properties);
	}

	@Override
	public void run() {
		log.info("Worker started");
		while (running) {
			try {
				String activeProfile = LuckyStackWorkerContext.getActiveProfile();
				if (activeProfile != null) {
					log.info("Applying profile {}", activeProfile);
					Collection<File> files = FileUtils.listFiles(Paths.get(getInputFolder()).toFile(), getExtensions(),
							true);
					LuckyStackWorkerContext.setTotalfilesCount(files.size());
					LuckyStackWorkerContext.setFilesProcessedCount(0);
					if (!processFiles(files)) {
						log.warn(
								"Worker did not process any file from {}, active profile {} not matching with any files",
								getInputFolder(), activeProfile);
					}
					LuckyStackWorkerContext.inactivateProfile();
					LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
					LuckyStackWorkerContext.setFilesProcessedCount(0);
					LuckyStackWorkerContext.setTotalfilesCount(0);
				} else {
					log.debug("Waiting for a profile to be applied...");
				}
				Util.pause(WAIT_DELAY);
			} catch (Exception e) {
				log.error("Error:", e);
				LuckyStackWorkerContext.inactivateProfile();
				LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
				LuckyStackWorkerContext.setFilesProcessedCount(0);
				LuckyStackWorkerContext.setTotalfilesCount(0);
			}
		}
	}

	public void stopRunning() {
		running = false;
	}

	private boolean processFiles(Collection<File> files) {
		boolean filesProcessed = false;
		int count = 0;
		for (File file : files) {
			String[] filename = Util.getFilename(file);
			String name = filename[0];
			String extension = filename[filename.length - 1];
			if (!name.endsWith(Constants.OUTPUT_POSTFIX) && Arrays.asList(getExtensions()).contains(extension)) {
				filesProcessed = filesProcessed | workerService.processFile(file);
			}
			LuckyStackWorkerContext.setFilesProcessedCount(++count);
		}
		return filesProcessed;
	}

	private String getInputFolder() {
		return properties.get("inputFolder");
	}

	private String[] getExtensions() {
		return properties.get("extensions").split(",");
	}
}
