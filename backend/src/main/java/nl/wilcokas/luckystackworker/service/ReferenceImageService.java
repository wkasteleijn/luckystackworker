package nl.wilcokas.luckystackworker.service;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.util.Operations;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Service
public class ReferenceImageService {

	private ImagePlus referenceImage;
	private ImagePlus processedImage;
	private ImagePlus finalResultImage;
	private OperationEnum previousOperation;
	private String filePath;

	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private SettingsRepository settingsRepository;

	public Profile selectReferenceImage(String filePath) {
		JFrame frame = getParentFrame();
		JFileChooser jfc = getJFileChooser(filePath);
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			String selectedFilePath = selectedFile.getAbsolutePath();
			log.info("Image selected {} ", selectedFilePath);

			String profileName = Util.deriveProfileFromImageName(selectedFilePath);
			if (profileName == null) {
				log.info("Profile not found for reference image, taking the default, {}", profileName);
				profileName = getSettings().getDefaultProfile();
			}

			Profile profile = profileRepository.findByName(profileName)
					.orElseThrow(() -> new ResourceNotFoundException("Unknown profile!"));
			openReferenceImage(selectedFilePath, profile);

			final String rootFolder = Util.getFileDirectory(selectedFilePath);
			updateSettings(rootFolder, profile);
			return profile;
		}
		return new Profile();
	}

	public void openReferenceImage(String filePath, Profile profile) {
		finalResultImage = IJ.openImage(Util.getIJFileFormat(filePath));
		if (finalResultImage != null) {
			Operations.applyInitialSettings(finalResultImage);
			log.info("Opened final result image image with id {}", finalResultImage.getID());
			finalResultImage.show(filePath);

			processedImage = finalResultImage.duplicate();
			log.info("Opened duplicate image with id {}", processedImage.getID());
			processedImage.show();

			referenceImage = processedImage.duplicate();
			referenceImage.show();
			log.info("Opened reference image image with id {}", referenceImage.getID());

			referenceImage.getWindow().setVisible(false);
			processedImage.getWindow().setVisible(false);

			if (profile != null) {
				updateProcessing(profile);
			}

			this.filePath = filePath;
			finalResultImage.setTitle(filePath);
		}
	}

	public void updateProcessing(Profile profile) {
		final OperationEnum operation = profile.getOperation() == null ? null
				: OperationEnum.valueOf(profile.getOperation().toUpperCase());
		if (previousOperation == null || previousOperation != operation) {
			copyInto(referenceImage, processedImage);
			if (Operations.isSharpenOperation(operation)) {
				Operations.applyAllOperationsExcept(processedImage, profile, operation, OperationEnum.DENOISE);
			} else {
				Operations.applyAllOperationsExcept(processedImage, profile, operation);
			}
			previousOperation = operation;
		}
		copyInto(processedImage, finalResultImage);

		if (Operations.isSharpenOperation(operation)) {
			Operations.applySharpen(finalResultImage, profile);
			Operations.applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.DENOISE == operation) {
			Operations.applyDenoise(finalResultImage, profile);
		} else if (OperationEnum.GAMMA == operation) {
			Operations.applyGamma(finalResultImage, profile);
		} else if (OperationEnum.RED == operation) {
			Operations.applyRed(finalResultImage, profile);
		} else if (OperationEnum.GREEN == operation) {
			Operations.applyGreen(finalResultImage, profile);
		} else if (OperationEnum.BLUE == operation) {
			Operations.applyBlue(finalResultImage, profile);
		}
		finalResultImage.setTitle(filePath);
	}

	public void saveReferenceImage(String path) {
		String dir = Util.getFileDirectory(filePath);
		log.info("Saving image to folder {}", dir);
		String finalPath = Util.getFilename(path)[0] + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
		IJ.save(finalResultImage, finalPath);
		log.info("Saved file to {}", finalPath);
	}

	public MyFileChooser getJFileChooser(String path) {
		MyFileChooser jfc = new MyFileChooser(path);
		jfc.requestFocus();
		return jfc;
	}

	public JFrame getParentFrame() {
		final JFrame frame = new JFrame();
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.requestFocus();
		return frame;
	}

	public void updateSettings(String rootFolder, Profile profile) {
		log.info("Setting the root folder to {}", rootFolder);
		Settings settings = getSettings();
		settings.setRootFolder(rootFolder);
		settingsRepository.save(settings);
		LuckyStackWorkerContext.updateWorkerForRootFolder(rootFolder);
		profile.setRootFolder(rootFolder);
		String selectedProfile = profile.getName();
		if (selectedProfile != null) {
			LuckyStackWorkerContext.setSelectedProfile(selectedProfile);
		}
	}

	public Settings getSettings() {
		return settingsRepository.findAll().iterator().next();
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
		destination.setTitle("PROCESSING");
	}

	final class MyFileChooser extends JFileChooser {

		MyFileChooser(String path) {
			super(path);
		}

		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			JDialog dlg = super.createDialog(parent);
			dlg.setLocation(110, 255);
			return dlg;
		}
	}

}
