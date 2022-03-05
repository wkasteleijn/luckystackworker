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
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			String selectedFilePath = selectedFile.getAbsolutePath();
			if (validateSelectedFile(selectedFilePath)) {
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
		}
		return new Profile();
	}

	public void updateProcessing(Profile profile) throws IOException {
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
		String finalPath = Util.getFilename(path)[0] + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
		Util.saveImage(finalResultImage, finalPath, this.filePath.toLowerCase().endsWith(".png"));
		log.info("Saved file to {}", finalPath);
	}

	public MyFileChooser getJFileChooser(String path) {
		MyFileChooser jfc = new MyFileChooser(path);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("TIFF, PNG", "tif", "png");
		jfc.setFileFilter(filter);
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

	private void openReferenceImage(String filePath, Profile profile) throws IOException {
		this.filePath = filePath;
		finalResultImage = new Opener().openImage(Util.getIJFileFormat(this.filePath));
		if (finalResultImage != null) {
			finalResultImage.show();
			finalResultImage.getWindow().setVisible(false);
			if (filePath.toLowerCase().endsWith(".png")) {
				finalResultImage = Util.fixNonTiffOpeningSettings(finalResultImage);
			}
			Operations.correctExposure(finalResultImage);
			log.info("Opened final result image image with id {}", finalResultImage.getID());
			setDefaultLayoutSettings(finalResultImage);

			processedImage = finalResultImage.duplicate();
			log.info("Opened duplicate image with id {}", processedImage.getID());
			processedImage.show();
			processedImage.getWindow().setVisible(false);
			Operations.correctExposure(processedImage);

			referenceImage = processedImage.duplicate();
			referenceImage.show();
			referenceImage.getWindow().setVisible(false);
			Operations.correctExposure(referenceImage);
			log.info("Opened reference image image with id {}", referenceImage.getID());

			finalResultImage.getWindow().setVisible(true);

			if (profile != null) {
				updateProcessing(profile);
			}

			finalResultImage.setTitle(this.filePath);
		}
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) throws IOException {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
		destination.setTitle("PROCESSING");
		Operations.correctExposure(destination);
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
