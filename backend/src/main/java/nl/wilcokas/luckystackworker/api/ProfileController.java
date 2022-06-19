package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdate;
import nl.wilcokas.luckystackworker.dto.Version;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.util.Util;

@CrossOrigin(origins = { "http://localhost:4200" })
@RestController
@RequestMapping("/api/profiles")
@Slf4j
public class ProfileController {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private ReferenceImageService referenceImageService;

	@GetMapping
	public List<Profile> getProfiles() {
		log.info("getProfiles called");
		return StreamSupport.stream(profileRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
	}

	@GetMapping("/selected")
	public Profile getSelectedProfile() {
		log.info("getSelectedProfile called");
		Profile profile = new Profile();
		String profileName = LuckyStackWorkerContext.getSelectedProfile();
		if (profileName != null) {
			profile = profileRepository.findByName(profileName)
					.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
		}
		profile.setRootFolder(referenceImageService.getSettings().getRootFolder());
		return profile;
	}

	@GetMapping("/{profile}")
	public Profile getProfile(@PathVariable(value = "profile") String profileName) {
		log.info("getProfile called with profile {}", profileName);
		Profile profile = profileRepository.findByName(profileName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
		return profile;
	}

	@GetMapping("/load")
	public Profile loadProfile() {
		log.info("loadProfile called");
		JFrame frame = referenceImageService.getParentFrame();
		JFileChooser jfc = referenceImageService
				.getJFileChooser(LuckyStackWorkerContext.getWorkerProperties().get("inputFolder"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML", "yaml");
		jfc.setFileFilter(filter);
		int returnValue = jfc.showOpenDialog(frame);
		frame.dispose();
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File selectedFile = jfc.getSelectedFile();
			String selectedFilePath = selectedFile.getAbsolutePath();
			String fileNameNoExt = Util.getFilename(selectedFilePath)[0];
			Profile profile = Util.readProfile(fileNameNoExt);
			if (profile != null) {
				updateProfile(profile);
				return profile;
			}
		}
		return null;
	}

	@PutMapping
	public ResponseEntity<String> updateProfile(@RequestBody Profile profile) {
		log.info("updateProfile called with profile {}", profile);
		Profile result = profileRepository.findByName(profile.getName())
				.orElseThrow(
						() -> new ResourceNotFoundException(String.format("Unknown profile %s", profile.getName())));
		result.setRadius(profile.getRadius());
		result.setAmount(profile.getAmount());
		result.setIterations(profile.getIterations());
		result.setLevel(profile.getLevel());
		result.setDenoise(profile.getDenoise());
		result.setDenoiseSigma(profile.getDenoiseSigma());
		result.setDenoiseRadius(profile.getDenoiseRadius());
		result.setGamma(profile.getGamma());
		result.setRed(profile.getRed());
		result.setGreen(profile.getGreen());
		result.setBlue(profile.getBlue());
		profileRepository.save(result);
		referenceImageService.updateProcessing(profile);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping("/apply")
	public void applyProfile(@RequestBody Profile profile) throws IOException {
		String profileName = profile.getName();
		if (profileName != null) {
			LuckyStackWorkerContext.statusUpdate(Constants.STATUS_WORKING);
			LuckyStackWorkerContext.updateWorkerForProfile(profile);
			LuckyStackWorkerContext.setActiveProfile(profileName);
			referenceImageService.writeProfile();
		} else {
			log.warn("Attempt to apply profile while nothing was selected");
			LuckyStackWorkerContext.statusUpdate(Constants.STATUS_IDLE);
		}
	}

	@GetMapping("/status")
	public StatusUpdate getStatus() {
		log.info("getStatus called");
		return LuckyStackWorkerContext.getStatus();
	}

	@GetMapping("/version")
	public Version getLatestVersion() {
		log.info("getLatestVersion called");
		String latestKnowVersion = referenceImageService.getSettings().getLatestKnownVersion();
		String latestVersionFromSite = referenceImageService.updateLatestVersion();
		if (latestVersionFromSite != null && !latestVersionFromSite.equals(latestKnowVersion)) {
			return Version.builder().latestVersion(latestVersionFromSite).isNewVersion(true).build();
		}
		return Version.builder().latestVersion(latestKnowVersion)
				.isNewVersion(false).build();
	}

	@PutMapping("/exit")
	public void exit() {
		log.info("Exit called, ending application");
		System.exit(0);
	}
}
