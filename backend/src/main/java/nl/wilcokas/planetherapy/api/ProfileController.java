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

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.planetherapy.PlanetherapyContext;
import nl.wilcokas.planetherapy.constants.Constants;
import nl.wilcokas.planetherapy.dto.StatusUpdate;
import nl.wilcokas.planetherapy.model.Profile;
import nl.wilcokas.planetherapy.repository.ProfileRepository;
import nl.wilcokas.planetherapy.service.ReferenceImageService;

@CrossOrigin(origins = "http://localhost:4200")
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

	@GetMapping("/{profile}")
	public Profile getProfile(@PathVariable(value = "profile") String profileName) {
		log.info("getProfile called with profile {}", profileName);
		Profile profile = profileRepository.findByName(profileName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
		return profile;
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
		result.setGamma(profile.getGamma());
		result.setRed(profile.getRed());
		result.setGreen(profile.getGreen());
		result.setBlue(profile.getBlue());
		profileRepository.save(result);
		referenceImageService.updateProcessing(profile);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PutMapping("/apply")
	public void applyProfile(@RequestBody Profile profile) {
		PlanetherapyContext.statusUpdate(Constants.STATUS_WORKING);
		PlanetherapyContext.updateWorkerForProfile(profile);
		PlanetherapyContext.setActiveProfile(profile.getName());
	}

	@GetMapping("/status")
	public StatusUpdate getStatus() {
		log.info("getStatus called");
		return PlanetherapyContext.getStatus();
	}

	@PutMapping("/exit")
	public void exit() {
		log.info("Exit called, ending application");
		System.exit(0);
	}
}
