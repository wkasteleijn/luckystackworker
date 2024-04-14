package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProfileService {
    private final ObjectMapper objectMapper;
    private static final String PROFILES_FILE = "/profiles.json";
    private static String defaultProfilesJson;
    static {
        try {
            defaultProfilesJson = LswFileUtil.readFromInputStream(new ClassPathResource(PROFILES_FILE).getInputStream());
        } catch (IOException e) {
            log.error("Error loading profiles.json",e);
        }
    }
    private Map<String, Profile> profiles;

    public void updateProfile(ProfileDTO profileDTO) {
        log.info("updateProfile called with profile {}", profileDTO);
        if (profiles == null) {
            readProfiles();
        }
        Profile profile = Profile.builder().build();
        profile.mapFromDTO(profileDTO);
        profiles.put(profile.getName(), profile);
        try {
            objectMapper.writeValue(new File(LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + PROFILES_FILE), profiles.values());
        } catch (Exception e) {
            log.error("Error writing profiles: ", e);
        }
    }

    public Optional<Profile> findByName(String profileName) {
        if (profiles == null) {
            readProfiles();
        }
        return Optional.ofNullable(profiles.get(profileName));
    }

    public Collection<Profile> getAllProfiles() {
        if (profiles == null) {
            readProfiles();
        }
        return profiles.values();
    }

    private void readProfiles() {
        String json = null;
        try {
            json = Files.readString(Paths.get(LswFileUtil.getDataFolder(LswUtil.getActiveOSProfile()) + PROFILES_FILE));
        } catch (Exception e) {
            log.warn("Profiles file not found");
        }
        if (json == null) {
            log.info("Reverting to the default profiles");
            json = defaultProfilesJson;
        }
        try {
            List<Profile> list = objectMapper.readValue(json, new TypeReference<List<Profile>>() {
            });
            profiles = list.stream().collect(Collectors.toMap(Profile::getName, Function.identity()));

        } catch (JsonProcessingException e) {
            log.error("Error reading profiles: ", e);
            throw new ProfileNotFoundException(e.getMessage());
        }
    }
}
