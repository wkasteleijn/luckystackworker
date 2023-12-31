package nl.wilcokas.luckystackworker.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;

    public void updateProfile(ProfileDTO profile) {
        log.info("updateProfile called with profile {}", profile);
        Profile result = profileRepository.findByName(profile.getName()).orElseThrow(
                () -> new ProfileNotFoundException(String.format("Unknown profile %s", profile.getName())));
        result.setRadius(profile.getRadius());
        result.setAmount(profile.getAmount());
        result.setIterations(profile.getIterations());
        result.setLevel(profile.getLevel());
        result.setIansAmount(profile.getIansAmount());
        result.setIansRecovery(profile.getIansRecovery());
        result.setDenoise1Amount(profile.getDenoise1Amount());
        result.setDenoise1Radius(profile.getDenoise1Radius());
        result.setDenoise1Iterations(profile.getDenoise1Iterations());
        result.setDenoise2Radius(profile.getDenoise2Radius());
        result.setDenoise2Iterations(profile.getDenoise2Iterations());
        result.setSavitzkyGolayAmount(profile.getSavitzkyGolayAmount());
        result.setSavitzkyGolaySize(profile.getSavitzkyGolaySize());
        result.setSavitzkyGolayIterations(profile.getSavitzkyGolayIterations());
        result.setClippingStrength(profile.getClippingStrength());
        result.setDeringStrength(profile.getDeringStrength());
        result.setDeringRadius(profile.getDeringRadius());
        result.setThreshold(profile.getDeringThreshold());
        result.setClippingRange(profile.getClippingRange());
        result.setSharpenMode(profile.getSharpenMode());
        result.setGamma(profile.getGamma());
        result.setRed(profile.getRed());
        result.setGreen(profile.getGreen());
        result.setBlue(profile.getBlue());
        result.setPurple(profile.getPurple());
        result.setSaturation(profile.getSaturation());
        result.setContrast(profile.getContrast());
        result.setBrightness(profile.getBrightness());
        result.setBackground(profile.getBackground());
        result.setLocalContrastMode(profile.getLocalContrastMode());
        result.setLocalContrastFine(profile.getLocalContrastFine());
        result.setLocalContrastMedium(profile.getLocalContrastMedium());
        result.setLocalContrastLarge(profile.getLocalContrastLarge());
        result.setDispersionCorrectionEnabled(profile.isDispersionCorrectionEnabled());
        result.setDispersionCorrectionRedX(profile.getDispersionCorrectionRedX());
        result.setDispersionCorrectionRedY(profile.getDispersionCorrectionRedY());
        result.setDispersionCorrectionBlueX(profile.getDispersionCorrectionBlueX());
        result.setDispersionCorrectionBlueY(profile.getDispersionCorrectionBlueY());
        result.setLuminanceIncludeRed(profile.isLuminanceIncludeRed());
        result.setLuminanceIncludeGreen(profile.isLuminanceIncludeGreen());
        result.setLuminanceIncludeBlue(profile.isLuminanceIncludeBlue());
        result.setLuminanceIncludeColor(profile.isLuminanceIncludeColor());
        result.setScale(profile.getScale());
        result.setEqualizeLocalHistogramsStrength(profile.getEqualizeLocalHistogramsStrength());
        profileRepository.save(result);
    }

    public Optional<Profile> findByName(String profileName) {
        return profileRepository.findByName(profileName);
    }

    public List<Profile> getAllProfiles() {
        return StreamSupport.stream(profileRepository.findAll().spliterator(), false).toList();
    }
}
