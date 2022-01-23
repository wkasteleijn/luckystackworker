package nl.wilcokas.planetherapy;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.worker.Worker;
import nl.wilcokas.planetherapy.worker.WorkerException;

public class PlanetherapyContext {
	private static Worker worker;
	private static Properties workerProperties;

	private PlanetherapyContext() {
	}

	public static Worker getWorker() throws IOException {
		if (worker == null) {
			worker = new Worker(workerProperties);
		}
		return worker;
	}

	public static Properties getWorkerProperties() {
		if (workerProperties == null) {
			throw new WorkerException("Properties not loaded");
		}
		return workerProperties;
	}

	public static void loadWorkerProperties(List<Profile> profiles) {
		if (workerProperties == null) {
			workerProperties = new Properties();
		}
		for (Profile profile:profiles) {
			workerProperties.setProperty(profile.getName() + ".amount=", String.valueOf(profile.getAmount()));
			// TODO
		}
	}
}
