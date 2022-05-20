package nl.wilcokas.luckystackworker.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.io.Opener;
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

	private Image iconImage = new ImageIcon(getClass().getResource("/luckystackworker_icon.png")).getImage();

	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private SettingsRepository settingsRepository;

	public Profile selectReferenceImage(String filePath) throws IOException {
		JFrame frame = getParentFrame();
		JFileChooser jfc = getJFileChooser(filePath);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TIFF, PNG", "tif", "png");
		jfc.setFileFilter(filter);
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			String selectedFilePath = selectedFile.getAbsolutePath();
			if (validateSelectedFile(selectedFilePath)) {
				log.info("Image selected {} ", selectedFilePath);
				String fileNameNoExt = Util.getFilename(selectedFilePath)[0];
				Profile profile = Util.readProfile(fileNameNoExt);
				if (profile == null) {
					String profileName = Util.deriveProfileFromImageName(selectedFilePath);
					if (profileName == null) {
						log.info("Profile not found for reference image, taking the default, {}", profileName);
						profileName = getSettings().getDefaultProfile();
					}
					profile = profileRepository.findByName(profileName)
							.orElseThrow(() -> new ResourceNotFoundException("Unknown profile!"));
				} else {
					log.info("Profile file found, profile was loaded from there.");
				}
				openReferenceImage(selectedFilePath, profile);

				final String rootFolder = Util.getFileDirectory(selectedFilePath);
				updateSettings(rootFolder, profile);
				return profile;
			}
		}
		return new Profile();
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

	public void saveReferenceImage(String path) throws IOException {
		String dir = Util.getFileDirectory(filePath);
		log.info("Saving image to folder {}", dir);
		String fileNameNoExt = Util.getFilename(path)[0];
		String finalPath = fileNameNoExt + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
		Util.saveImage(finalResultImage, finalPath, Util.isPngRgbStack(finalResultImage, filePath));
		log.info("Saved file to {}", finalPath);
		writeProfile(fileNameNoExt);
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
		frame.setIconImage(iconImage);
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

	public void zoomIn() {
		IJ.run(finalResultImage, "In [+]", null);
	}

	public void zoomOut() {
		IJ.run(finalResultImage, "Out [-]", null);
	}

	public Settings getSettings() {
		return settingsRepository.findAll().iterator().next();
	}

	public void writeProfile() throws IOException {
		String fileNameNoExt = Util.getFilename(filePath)[0];
		writeProfile(fileNameNoExt);
	}

	public void writeProfile(String fileNameNoExt) throws IOException {
		String profileName = LuckyStackWorkerContext.getSelectedProfile();
		if (profileName != null) {
			Profile profile = profileRepository.findByName(profileName)
					.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
			Util.writeProfile(profile, fileNameNoExt);
		} else {
			log.warn("Profile not saved, could not find the selected profile for file {}", fileNameNoExt);
		}
	}

	public String getFilePath() {
		return filePath;
	}

	private void openReferenceImage(String filePath, Profile profile) throws IOException {
		this.filePath = filePath;
		finalResultImage = new Opener().openImage(Util.getIJFileFormat(this.filePath));
		if (finalResultImage != null) {
			if (Util.isPngRgbStack(finalResultImage, filePath)) {
				finalResultImage = Util.fixNonTiffOpeningSettings(finalResultImage);
			}
			Operations.correctExposure(finalResultImage);
			log.info("Opened final result image image with id {}", finalResultImage.getID());
			setDefaultLayoutSettings(finalResultImage);

			processedImage = finalResultImage.duplicate();
			log.info("Opened duplicate image with id {}", processedImage.getID());
			processedImage.show();
			processedImage.getWindow().setVisible(false);

			referenceImage = processedImage.duplicate();
			referenceImage.show();
			referenceImage.getWindow().setVisible(false);
			log.info("Opened reference image image with id {}", referenceImage.getID());


			if (profile != null) {
				updateProcessing(profile);
			}

			finalResultImage.setTitle(this.filePath);
		}
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
		destination.setTitle("PROCESSING");
	}

	private void setDefaultLayoutSettings(ImagePlus image) {
		image.setColor(Color.BLACK);
		image.setBorderColor(Color.BLACK);
		image.show(filePath);
		ImageWindow window = image.getWindow();
		window.setIconImage(iconImage);
		window.setLocation(742, 64);
		if (image.getWidth() > 1280) {
			zoomOut();
		}
	}

	private boolean validateSelectedFile(String path) {
		String extension = Util.getFilename(path)[1].toLowerCase();
		if (!getSettings().getExtensions().contains(extension)) {
			JOptionPane.showMessageDialog(getParentFrame(),
					String.format(
							"The selected file with extension %s is not supported. %nYou can only open 16-bit RGB and Gray PNG and TIFF images.",
							extension));
			return false;
		}
		return true;
	}

	final class MyFileChooser extends JFileChooser {

		MyFileChooser(String path) {
			super(path);
		}

		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			JDialog dlg = super.createDialog(parent);
			dlg.setLocation(108, 255);
			return dlg;
		}
	}

}
