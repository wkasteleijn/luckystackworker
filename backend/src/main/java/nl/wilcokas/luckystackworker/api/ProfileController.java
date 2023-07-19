package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.StatusUpdate;
import nl.wilcokas.luckystackworker.dto.Version;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.ProfileService;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.service.SettingsService;
import nl.wilcokas.luckystackworker.util.Util;

@CrossOrigin(origins = { "http://localhost:4200" })
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/profiles")
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final ReferenceImageService referenceImageService;
    private final SettingsService settingsService;

    @GetMapping
    public List<Profile> getProfiles() {
        log.info("getProfiles called");
        return profileService.getAllProfiles();
    }

    @GetMapping("/selected")
    public Profile getSelectedProfile() {
        log.info("getSelectedProfile called");
        Profile profile = new Profile();
        String profileName = LuckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            profile = profileService.findByName(profileName)
                    .orElseThrow(() -> new ProfileNotFoundException(String.format("Unknown profile %s", profileName)));
        }

        // This is not persisted, so get state from ref.service
        profile.setRootFolder(settingsService.getRootFolder());
        profile.setLargeImage(referenceImageService.isLargeImage());

        return profile;
    }

    @GetMapping("/{profile}")
    public Profile getProfile(@PathVariable(value = "profile") String profileName) {
        log.info("getProfile called with profile {}", profileName);
        return profileService.findByName(profileName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
    }

    @GetMapping("/load")
    public Profile loadProfile() {
        log.info("loadProfile called");
        JFrame frame = referenceImageService.getParentFrame();
        JFileChooser jfc = referenceImageService
                .getJFileChooser(settingsService.getRootFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML", "yaml");
        jfc.setFileFilter(filter);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String selectedFilePath = selectedFile.getAbsolutePath();
            String filePathNoExt = Util.getPathWithoutExtension(selectedFilePath);
            Profile profile = Util.readProfile(filePathNoExt);
            if (profile != null) {
                profile.setLargeImage(referenceImageService.isLargeImage());
                profile.setDispersionCorrectionEnabled(false); // Dispersion correction is not meant to be persisted.
                updateProfile(profile);
                LuckyStackWorkerContext.setSelectedProfile(profile.getName());
                return profile;
            }
        }
        return null;
    }

    @PutMapping
    public ResponseEntity<String> updateProfile(@RequestBody Profile profile) {
        // Rate limiting added to prevent overloading whenever scroll keys are held down
        // or pressed very quickly.
        LocalDateTime activeOperationTime = LuckyStackWorkerContext.getActiveOperationTime();
        if (activeOperationTime == null
                || LocalDateTime.now()
                .isAfter(activeOperationTime.plusSeconds(Constants.MAX_OPERATION_TIME_BEFORE_RESUMING))) {
            LuckyStackWorkerContext.setActiveOperationTime(LocalDateTime.now());
            profileService.updateProfile(profile);
            referenceImageService.updateProcessing(profile);
            LuckyStackWorkerContext.setActiveOperationTime(null);
        } else {
            log.warn("Attempt to update image while another operation was in progress");
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/apply")
    public void applyProfile(@RequestBody Profile profile) throws IOException {
        String profileName = profile.getName();
        if (profileName != null) {
            LuckyStackWorkerContext.statusUpdate(Constants.STATUS_WORKING);
            LuckyStackWorkerContext.setSelectedRoi(referenceImageService.getFinalResultImage().getRoi());
            LuckyStackWorkerContext.setSelectedProfile(profileName);
            referenceImageService.writeProfile();
            LuckyStackWorkerContext.setProfileBeingApplied(true);
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
        return referenceImageService.getLatestVersion(LocalDateTime.now());
    }

    @PutMapping("/exit")
    public void exit() {
        log.info("Exit called, ending application");
        if (referenceImageService.getFinalResultImage() != null) {
            referenceImageService.getFinalResultImage().hide();
        }
        System.exit(0);
    }
}
