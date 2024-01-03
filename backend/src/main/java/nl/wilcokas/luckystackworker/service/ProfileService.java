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
        result.mapFromDTO(profile);
        profileRepository.save(result);
    }

    public Optional<Profile> findByName(String profileName) {
        return profileRepository.findByName(profileName);
    }

    public List<Profile> getAllProfiles() {
        return StreamSupport.stream(profileRepository.findAll().spliterator(), false).toList();
    }
}
