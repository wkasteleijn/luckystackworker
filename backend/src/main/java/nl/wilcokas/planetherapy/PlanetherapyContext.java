package nl.wilcokas.planetherapy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.model.Settings;
import nl.wilcokas.planetherapy.worker.Worker;
import nl.wilcokas.planetherapy.worker.WorkerException;

public class PlanetherapyContext {
	private static Worker worker;
	private static Map<String, String> workerProperties;
	private static String activeProfile;

	private PlanetherapyContext() {
	}

	public static Worker getWorker() throws IOException {
		if (worker == null) {
			worker = new Worker(workerProperties);
		}
		return worker;
	}

	public static Map<String, String> getWorkerProperties() {
		if (workerProperties == null) {
			throw new WorkerException("Properties not loaded");
		}
		return workerProperties;
	}

	public static void loadWorkerProperties(Iterator<Profile> profiles, Settings settings) {
		if (workerProperties == null) {
			workerProperties = new HashMap<String, String>();
		}
		while (profiles.hasNext()) {
			Profile profile = profiles.next();
			updateWorkerForProfile(profile);
		}
		workerProperties.put("inputFolder", settings.getRootFolder());
		workerProperties.put("extensions", settings.getExtensions());
		workerProperties.put("outputFormat", settings.getOutputFormat());
		workerProperties.put("defaultProfile", settings.getDefaultProfile());
	}

	public static void updateWorkerForProfile(Profile profile) {
		String name = profile.getName();
		workerProperties.put(name + ".radius", String.valueOf(profile.getRadius()));
		workerProperties.put(name + ".amount", String.valueOf(profile.getAmount()));
		workerProperties.put(name + ".iterations", String.valueOf(profile.getIterations()));
		workerProperties.put(name + ".denoise", String.valueOf(profile.getDenoise()));
		workerProperties.put(name + ".gamma", String.valueOf(profile.getGamma()));
		workerProperties.put(name + ".red", String.valueOf(profile.getRed()));
		workerProperties.put(name + ".green", String.valueOf(profile.getGreen()));
		workerProperties.put(name + ".blue", String.valueOf(profile.getBlue()));
	}

	public static String getActiveProfile() {
		return activeProfile;
	}

	public static void setActiveProfile(String profile) {
		activeProfile = profile;
	}

	public static void inactivateProfile() {
		activeProfile = null;
	}
}
