package nl.wilcokas.luckystackworker.service;

import java.util.Optional;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;

@Service
@Slf4j
public class ProfileService {

	@Autowired
	private ProfileRepository profileRepository;

	public void updateProfile(Profile profile) {
		log.info("updateProfile called with profile {}", profile);
		Profile result = profileRepository.findByName(profile.getName()).orElseThrow(
				() -> new ResourceNotFoundException(String.format("Unknown profile %s", profile.getName())));
		result.setRadius(profile.getRadius());
		result.setAmount(profile.getAmount());
		result.setIterations(profile.getIterations());
		result.setLevel(profile.getLevel());
		result.setDenoise(profile.getDenoise());
		result.setDenoiseSigma(profile.getDenoiseSigma());
		result.setDenoiseRadius(profile.getDenoiseRadius());
		result.setDenoiseIterations(profile.getDenoiseIterations());
		result.setGamma(profile.getGamma());
		result.setRed(profile.getRed());
		result.setGreen(profile.getGreen());
		result.setBlue(profile.getBlue());
		result.setSaturation(profile.getSaturation());
		result.setContrast(profile.getContrast());
		result.setBrightness(profile.getBrightness());
		result.setBackground(profile.getBackground());
		profileRepository.save(result);
	}

	public Optional<Profile> findByName(String profileName) {
		return profileRepository.findByName(profileName);
	}
}
