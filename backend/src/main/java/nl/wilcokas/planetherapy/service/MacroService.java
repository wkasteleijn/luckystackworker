package nl.wilcokas.planetherapy.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.constants.Constants;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.util.Util;

@Slf4j
public class MacroService {

	private Map<String, String> parameters;
	private String fileContents;
	private String outputFormat;

	public MacroService() throws IOException {
		parameters = new HashMap<String, String>();
		this.fileContents = getMacroFileContent();
	}

	public MacroService(Properties properties) throws IOException {
		this.outputFormat = properties.getProperty("outputFormat");
		parameters = properties.keySet().stream()
				.collect(Collectors.toMap(p -> (String) p, p -> (String) properties.get(p)));
		this.fileContents = getMacroFileContent();
	}

	public void runMacro(File file) {
		String profile = getProfile(file);
		log.info(
				String.format("Applying sharpen filter with profile '%s' to: %s", profile, file.getAbsolutePath()));

		ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		fillParameters(file, profile, imp.isStack());
		StringSubstitutor stringSubstitutor = new StringSubstitutor(parameters);
		String result = stringSubstitutor.replace(fileContents);
		IJ.runMacro(result);
	}

	public void runMacro(ImagePlus imp, Profile profile, String inputFilePath) {
		fillParameters(profile, inputFilePath, imp.isStack());
		StringSubstitutor stringSubstitutor = new StringSubstitutor(parameters);
		String result = stringSubstitutor.replace(fileContents);
		IJ.runMacro(result);
	}

	private void fillParameters(File file, String profile, boolean isStack) {
		parameters.put("outputFile", Util.getIJFileFormat(getOutputFile(file)));
		parameters.put("inputFile", Util.getIJFileFormat(file.getAbsolutePath()));
		parameters.put("radius", parameters.get(profile + ".radius"));
		parameters.put("amount", parameters.get(profile + ".amount"));
		parameters.put("iterations", parameters.get(profile + ".iterations"));
		parameters.put("level", parameters.get(profile + ".level"));
		parameters.put("denoise", parameters.get(profile + ".denoise"));
		parameters.put("isStack", String.valueOf(isStack));
	}

	private void fillParameters(Profile profile, String inputFilePath, boolean isStack) {
		parameters.put("outputFile", Util.getProcessedFileName(inputFilePath));
		parameters.put("inputFile", Util.getIJFileFormat(inputFilePath));
		parameters.put("radius", profile.getRadius().toPlainString());
		parameters.put("amount", profile.getAmount().divide(new BigDecimal(10000)).toPlainString());
		parameters.put("iterations", String.valueOf(profile.getIterations()));
		parameters.put("level", String.valueOf(profile.getLevel()));
		parameters.put("denoise", profile.getDenoise().toPlainString());
		parameters.put("isStack", String.valueOf(isStack));
	}

	private String getProfile(File file) {
		String filename[] = file.getName().toString().split("_");
		String profile = null;
		if (filename.length > 0) {
			String profilePart = filename[0].toLowerCase();
			if (Constants.KNOWN_PROFILES.contains(profilePart)) {
				profile = profilePart.toLowerCase();
			}
		}
		return profile == null ? parameters.get("default.profile").toLowerCase() : profile;
	}

	private String getOutputFile(File file) {
		String[] filename = Util.getFilename(file);
		return filename[0] + Constants.OUTPUT_POSTFIX + "." + outputFormat;
	}

	private String getMacroFileContent() throws IOException {
		return readFromInputStream(this.getClass().getResourceAsStream("/macro.ijm"));
	}

	private String readFromInputStream(InputStream inputStream) throws IOException {
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

}
