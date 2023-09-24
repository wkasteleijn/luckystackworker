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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.dto.ResponseDTO;
import nl.wilcokas.luckystackworker.dto.SettingsDTO;
import nl.wilcokas.luckystackworker.dto.StatusUpdateDTO;
import nl.wilcokas.luckystackworker.dto.VersionDTO;
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
    public List<ProfileDTO> getProfiles() {
        log.info("getProfiles called");
        return profileService.getAllProfiles().stream().map(ProfileDTO::new).toList();
    }

    @GetMapping("/selected")
    public ResponseDTO getSelectedProfile() {
        log.info("getSelectedProfile called");
        ProfileDTO profile = new ProfileDTO();
        String profileName = LuckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            profile = new ProfileDTO(profileService.findByName(profileName)
                    .orElseThrow(() -> new ProfileNotFoundException(String.format("Unknown profile %s", profileName))));
        }
        SettingsDTO settings = new SettingsDTO(settingsService.getSettings());
        settings.setRootFolder(settingsService.getRootFolder());
        settings.setLargeImage(referenceImageService.isLargeImage());
        return new ResponseDTO(profile, settings);
    }

    @GetMapping("/{profile}")
    public ProfileDTO getProfile(@PathVariable(value = "profile") String profileName) {
        log.info("getProfile called with profile {}", profileName);
        return new ProfileDTO(profileService.findByName(profileName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName))));
    }

    @GetMapping("/load")
    public ResponseDTO loadProfile() {
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
                SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
                settingsDTO.setLargeImage(referenceImageService.isLargeImage());
                Util.setNonPersistentSettings(profile);
                ProfileDTO profileDTO = new ProfileDTO(profile);
                updateProfile(profileDTO, null);
                LuckyStackWorkerContext.setSelectedProfile(profile.getName());
                return new ResponseDTO(profileDTO, settingsDTO);
            }
        }
        return null;
    }

    @PutMapping
    public ResponseEntity<String> updateProfile(@RequestBody ProfileDTO profile, @RequestParam String operation) {
        // Rate limiting added to prevent overloading whenever scroll keys are held down
        // or pressed very quickly.
        LocalDateTime activeOperationTime = LuckyStackWorkerContext.getActiveOperationTime();
        if (activeOperationTime == null
                || LocalDateTime.now()
                .isAfter(activeOperationTime.plusSeconds(Constants.MAX_OPERATION_TIME_BEFORE_RESUMING))) {
            LuckyStackWorkerContext.setActiveOperationTime(LocalDateTime.now());
            profileService.updateProfile(profile);
            referenceImageService.updateProcessing(new Profile(profile), operation);
            LuckyStackWorkerContext.setActiveOperationTime(null);
        } else {
            log.warn("Attempt to update image while another operation was in progress");
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/apply")
    public void applyProfile(@RequestBody ProfileDTO profile) throws IOException {
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
    public StatusUpdateDTO getStatus() {
        log.info("getStatus called");
        return LuckyStackWorkerContext.getStatus();
    }

    @GetMapping("/version")
    public VersionDTO getLatestVersion() {
        log.info("getLatestVersion called");
        return referenceImageService.getLatestVersion(LocalDateTime.now());
    }

    @PutMapping("/scale")
    public ResponseDTO scale(@RequestBody ProfileDTO profile) throws IOException {
        profileService.updateProfile(profile);
        return referenceImageService.scale(new Profile(profile));
    }

    @PutMapping("/exit")
    public void exit() {
        log.info("Exit called, ending application");
        if (referenceImageService.getFinalResultImage() != null) {
            referenceImageService.getFinalResultImage().hide();
        }
        if (referenceImageService.getPlotWindow() != null) {
            referenceImageService.getPlotWindow().setVisible(false);
        }

        System.exit(0);
    }
}
