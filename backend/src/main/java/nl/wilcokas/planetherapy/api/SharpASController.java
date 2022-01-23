package nl.wilcokas.planetherapy.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

import ij.io.OpenDialog;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.repository.ProfileRepository;
import nl.wilcokas.planetherapy.service.ReferenceImageService;
import nl.wilcokas.planetherapy.util.Util;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
@Slf4j
public class SharpASController {

	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private ReferenceImageService referenceImageService;

	@GetMapping("/profiles")
	public List<Profile> getProfiles() {
		log.info("getProfiles called");
		return StreamSupport.stream(profileRepository.findAll().spliterator(), false)
				.collect(Collectors.toList());
	}

	@GetMapping("/profiles/{profile}")
	public Profile getProfile(@PathVariable(value = "profile") String profile) {
		log.info("getProfile called with profile {}", profile);
		return profileRepository.findByName(profile)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profile)));
	}

	@PutMapping("/profiles")
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
		profileRepository.save(result);
		referenceImageService.updateProcessing(profile);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping("/reference/open")
	public String openReferenceImage(@RequestBody String path) {
		OpenDialog od = new OpenDialog("Open Image", null);
		String referenceImage = od.getDirectory() + od.getFileName();
		log.info("Image selected {} ", referenceImage);
		String profileName = Util.deriveProfileFromImageName(referenceImage);
		Profile profile = null;
		if (profileName != null) {
			profile = profileRepository.findByName(profileName)
					.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
		}
		referenceImageService.openReferenceImage(referenceImage, profile);
		return profileName;
	}
}
