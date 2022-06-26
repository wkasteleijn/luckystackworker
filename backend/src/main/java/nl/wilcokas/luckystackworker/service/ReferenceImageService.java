package nl.wilcokas.luckystackworker.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDateTime;

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
import ij.gui.Toolbar;
import ij.io.Opener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.Crop;
import nl.wilcokas.luckystackworker.dto.Version;
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

	@Getter
	private ImagePlus finalResultImage;

	private OperationEnum previousOperation;
	private String filePath;
	private Crop crop = null;

	private Image iconImage = new ImageIcon(getClass().getResource("/luckystackworker_icon.png")).getImage();

	@Autowired
	private ProfileRepository profileRepository;
	@Autowired
	private SettingsRepository settingsRepository;
	@Autowired
	private HttpService httpService;

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
				boolean isLargeImage = openReferenceImage(selectedFilePath, profile);

				final String rootFolder = Util.getFileDirectory(selectedFilePath);
				updateSettings(rootFolder, profile);
				profile.setLargeImage(isLargeImage);
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
				Operations.applyAllOperationsExcept(processedImage, profile, operation, OperationEnum.DENOISEAMOUNT,
						OperationEnum.DENOISERADIUS, OperationEnum.DENOISESIGMA, OperationEnum.DENOISEITERATIONS);
			} else {
				Operations.applyAllOperationsExcept(processedImage, profile, operation);
			}
			previousOperation = operation;
		}
		copyInto(processedImage, finalResultImage);

		if (Operations.isSharpenOperation(operation)) {
			Operations.applySharpen(finalResultImage, profile);
			Operations.applyDenoise(finalResultImage, profile);
		} else if (Operations.isDenoiseOperation(operation)) {
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

		// Exception for saturation, always apply it as last as it will convert to RGB
		// color.
		ImagePlus duplicatedImage = Operations.applySaturation(finalResultImage, profile);
		if (duplicatedImage != null) {
			duplicatedImage.show();
			Point location = finalResultImage.getWindow().getLocation();
			finalResultImage.getWindow().setVisible(false);
			finalResultImage.close();
			finalResultImage = duplicatedImage;
			setDefaultLayoutSettings(finalResultImage, location);
		}

		finalResultImage.setTitle(filePath);
	}

	public void saveReferenceImage(String path) throws IOException {
		String dir = Util.getFileDirectory(filePath);
		log.info("Saving image to folder {}", dir);
		String fileNameNoExt = Util.getFilename(path)[0];
		String finalPath = fileNameNoExt + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
		Util.saveImage(finalResultImage, finalPath, Util.isPngRgbStack(finalResultImage, filePath), crop != null);
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

	public void crop() {
		if (crop == null) {
			int width = finalResultImage.getWidth() / 2;
			int height = finalResultImage.getHeight() / 2;
			int x = (finalResultImage.getWidth() - width) / 2;
			int y = (finalResultImage.getHeight() - height) / 2;
			crop = new Crop(x, y, width, height);
			finalResultImage.setRoi(x, y, width, height);
			new Toolbar().setTool(Toolbar.CROSSHAIR);
			LuckyStackWorkerContext.setSelectedRoi(finalResultImage.getRoi());
		} else {
			crop = null;
			finalResultImage.resetRoi();
			new Toolbar().setTool(Toolbar.HAND);
			LuckyStackWorkerContext.setSelectedRoi(null);
		}
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

	public Version getLatestVersion(LocalDateTime currentDate) {
		Settings settings = getSettings();
		String latestKnowVersion = settings.getLatestKnownVersion();
		if (settings.getLatestKnownVersionChecked() == null || currentDate
				.isAfter(settings.getLatestKnownVersionChecked().plusDays(Constants.VERSION_REQUEST_FREQUENCY))) {
			String latestVersionFromSite = requestLatestVersion();
			if (latestVersionFromSite != null) {
				settings.setLatestKnownVersion(latestVersionFromSite);
			}
			settings.setLatestKnownVersionChecked(currentDate);
			settingsRepository.save(settings);

			if (latestVersionFromSite != null && !latestVersionFromSite.equals(latestKnowVersion)) {
				return Version.builder().latestVersion(latestVersionFromSite).isNewVersion(true).build();
			}
		}
		return Version.builder().latestVersion(latestKnowVersion).isNewVersion(false).build();
	}

	private String requestLatestVersion() {

		// Retrieve version document
		String result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL,
				Constants.VERSION_REQUEST_TIMEOUT);
		if (result == null) {
			log.warn("HTTP1.1 request for latest version failed, trying HTTP/2..");
			result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_2, Constants.VERSION_URL,
					Constants.VERSION_REQUEST_TIMEOUT);
			if (result == null) {
				log.warn("HTTP/2 request for latest version failed as well");
			}
		}

		// Extract version
		String version = null;
		if (result != null) {
			version = getLatestVersion(result);
		}
		return version;
	}

	private String getLatestVersion(String htmlResponse) {
		int start = htmlResponse.indexOf(Constants.VERSION_URL_MARKER);
		if (start > 0) {
			int startVersionPos = start + Constants.VERSION_URL_MARKER.length();
			int endMarkerPos = htmlResponse.indexOf(Constants.VERSION_URL_ENDMARKER, startVersionPos);
			endMarkerPos = endMarkerPos < 0 ? startVersionPos : endMarkerPos; // robustness, don't fail if end marker
			// was missing
			String version = htmlResponse.substring(startVersionPos, endMarkerPos);
			if (validateVersion(version)) {
				log.info("Received valid version from server : {}", version);
				return version;
			} else {
				log.warn("Received an invalid version from the server");
			}
		}
		log.warn("Could not read the version from the server response to {}", Constants.VERSION_URL);
		return null;
	}

	private boolean validateVersion(String version) {
		if (version == null || version.length() == 0) {
			log.warn("Received an empty version from server : {}", version);
			return false;
		}
		if (version.length() > 10) { // 10.100.100 will never be reached :)
			log.warn("Received an invalid version nr from server : {}", version);
			return false;
		}
		return true;
	}

	private boolean openReferenceImage(String filePath, Profile profile) throws IOException {
		this.filePath = filePath;
		finalResultImage = new Opener().openImage(Util.getIJFileFormat(this.filePath));
		boolean isLargeImage = false;
		if (finalResultImage != null) {
			if (Util.isPngRgbStack(finalResultImage, filePath)) {
				finalResultImage = Util.fixNonTiffOpeningSettings(finalResultImage);
			}
			Operations.correctExposure(finalResultImage);
			log.info("Opened final result image image with id {}", finalResultImage.getID());
			isLargeImage = setDefaultLayoutSettings(finalResultImage, null);

			processedImage = finalResultImage.duplicate();
			// processedImage.setRoi(finalResultImage.getRoi());
			log.info("Opened duplicate image with id {}", processedImage.getID());
			processedImage.show();
			processedImage.getWindow().setVisible(false);

			referenceImage = processedImage.duplicate();
			// processedImage.setRoi(processedImage.getRoi());
			referenceImage.show();
			referenceImage.getWindow().setVisible(false);
			log.info("Opened reference image image with id {}", referenceImage.getID());


			if (profile != null) {
				updateProcessing(profile);
			}

			finalResultImage.setTitle(this.filePath);
		}
		return isLargeImage;
	}

	private void copyInto(final ImagePlus origin, final ImagePlus destination) {
		log.info("Copying image {} into image {}", origin.getID(), destination.getID());
		destination.setImage(origin);
		destination.setTitle("PROCESSING");
	}

	private boolean setDefaultLayoutSettings(ImagePlus image, Point location) {
		image.setColor(Color.BLACK);
		image.setBorderColor(Color.BLACK);
		image.show(filePath);
		ImageWindow window = image.getWindow();
		window.setIconImage(iconImage);
		if (location == null) {
			window.setLocation(742, 64);
		} else {
			window.setLocation(location);
		}
		new Toolbar().setTool(Toolbar.HAND);
		crop = null;
		if (image.getWidth() > Constants.MAX_WINDOW_SIZE) {
			// setRoi(image);
			zoomOut();
			return true;
		}
		return false;
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
