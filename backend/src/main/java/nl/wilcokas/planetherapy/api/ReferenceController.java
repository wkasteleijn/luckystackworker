package nl.wilcokas.planetherapy.api;

import java.io.File;
import java.util.Base64;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.PlanetherapyContext;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.model.Settings;
import nl.wilcokas.planetherapy.repository.ProfileRepository;
import nl.wilcokas.planetherapy.repository.SettingsRepository;
import nl.wilcokas.planetherapy.service.ReferenceImageService;
import nl.wilcokas.planetherapy.util.Util;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private ReferenceImageService referenceImageService;
	@Autowired
	private SettingsRepository settingsRepository;

	@GetMapping("/open")
	public Profile openReferenceImage(@RequestParam String path) {
		JFrame frame = getParentFrame();
		final String base64DecodedPath = new String(Base64.getDecoder().decode(path));
		JFileChooser jfc = getJFileChooser(base64DecodedPath);
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			String referenceImage = selectedFile.getAbsolutePath();
			log.info("Image selected {} ", referenceImage);
			String profileName = Util.deriveProfileFromImageName(referenceImage);
			Profile profile = null;
			if (profileName != null) {
				profile = profileRepository.findByName(profileName).orElseThrow(
						() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
			}
			referenceImageService.openReferenceImage(referenceImage, profile);
			updateSettings(referenceImage);
			return profile;
		}
		return null;
	}

	@PutMapping("/save")
	public void saveReferenceImage(@RequestBody String path) {
		JFrame frame = getParentFrame();
		JFileChooser jfc = getJFileChooser(path);
		int returnValue = jfc.showDialog(frame, "Save reference image");
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			referenceImageService.saveReferenceImage(selectedFile.getAbsolutePath());
		}
	}

	private JFileChooser getJFileChooser(String path) {
		JFileChooser jfc = new JFileChooser(path);
		jfc.requestFocus();
		return jfc;
	}

	private JFrame getParentFrame() {
		final JFrame frame = new JFrame();
		frame.setAlwaysOnTop(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.requestFocus();
		return frame;
	}

	private void updateSettings(String filePath) {
		final String rootFolder = Util.getFileDirectory(filePath);
		log.info("Setting the root folder to {}", rootFolder);
		Settings settings = settingsRepository.findAll().iterator().next();
		settings.setRootFolder(rootFolder);
		settingsRepository.save(settings);
		PlanetherapyContext.updateWorkerForRootFolder(rootFolder);
	}

}
