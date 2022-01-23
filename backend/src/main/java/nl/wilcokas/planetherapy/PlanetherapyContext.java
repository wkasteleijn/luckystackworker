package nl.wilcokas.planetherapy;

import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.model.Settings;
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

	public static void loadWorkerProperties(Iterator<Profile> profiles, Settings settings) {
		if (workerProperties == null) {
			workerProperties = new Properties();
		}
		while (profiles.hasNext()) {
			Profile profile = profiles.next();
			updateWorkerForProfile(profile);
		}
		workerProperties.setProperty("inputFolder", settings.getRootFolder());
		workerProperties.setProperty("extensions", settings.getExtensions());
		workerProperties.setProperty("outputFormat", settings.getOutputFormat());
		workerProperties.setProperty("defaultProfile", settings.getDefaultProfile());
	}

	public static void updateWorkerForProfile(Profile profile) {
		String name = profile.getName();
		workerProperties.setProperty(name + ".radius=", String.valueOf(profile.getRadius()));
		workerProperties.setProperty(name + ".amount=", String.valueOf(profile.getAmount()));
		workerProperties.setProperty(name + ".iterations=", String.valueOf(profile.getIterations()));
		workerProperties.setProperty(name + ".denoise=", String.valueOf(profile.getDenoise()));
		workerProperties.setProperty(name + ".gamma=", String.valueOf(profile.getGamma()));
		workerProperties.setProperty(name + ".red=", String.valueOf(profile.getRed()));
		workerProperties.setProperty(name + ".green=", String.valueOf(profile.getGreen()));
		workerProperties.setProperty(name + ".blue=", String.valueOf(profile.getBlue()));
	}
}
