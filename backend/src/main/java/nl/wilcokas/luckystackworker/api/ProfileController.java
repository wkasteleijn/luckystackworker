package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.*;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.PSFService;
import nl.wilcokas.luckystackworker.repository.ProfileRepository;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:4200"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/profiles")
@Slf4j
public class ProfileController {

    private final ProfileRepository profileService;
    private final ReferenceImageService referenceImageService;
    private final SettingsRepository settingsService;
    private final PSFService psfService;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    @GetMapping
    public List<ProfileDTO> getProfiles() {
        log.info("getProfiles called");
        return profileService.getAllProfiles().stream().map(ProfileDTO::new).toList();
    }

    @GetMapping("/selected")
    public ResponseDTO getSelectedProfile() {
        log.info("getSelectedProfile called");
        ProfileDTO profile = new ProfileDTO();
        String profileName = luckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            profile = new ProfileDTO(profileService
                    .findByName(profileName)
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
        return new ProfileDTO(profileService
                .findByName(profileName)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName))));
    }

    @GetMapping("/load")
    public ResponseDTO loadProfile() {
        log.info("loadProfile called");
        JFrame frame = referenceImageService.getParentFrame();
        JFileChooser jfc = referenceImageService.getJFileChooser(settingsService.getRootFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML", "yaml");
        jfc.setFileFilter(filter);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String selectedFilePath = selectedFile.getAbsolutePath();
            String filePathNoExt = LswFileUtil.getPathWithoutExtension(selectedFilePath);
            Profile profile = LswFileUtil.readProfile(filePathNoExt);
            if (profile != null) {
                SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
                settingsDTO.setLargeImage(referenceImageService.isLargeImage());
                LswImageProcessingUtil.setNonPersistentSettings(profile);
                ProfileDTO profileDTO = new ProfileDTO(profile);
                updateProfile(profileDTO, null);
                luckyStackWorkerContext.setSelectedProfile(profile.getName());
                return new ResponseDTO(profileDTO, settingsDTO);
            }
        }
        return null;
    }

    @PutMapping
    public ResponseEntity<UpdateProfileResponseDto> updateProfile(
            @RequestBody ProfileDTO profileDTO, @RequestParam(required = false) List<String> operations) {
        // Rate limiting added to prevent overloading whenever scroll keys are held down
        // or pressed very quickly.
        LocalDateTime activeOperationTime = luckyStackWorkerContext.getActiveOperationTime();
        byte[] psfImage = null;
        Profile profile = null;
        if (activeOperationTime == null
                || LocalDateTime.now()
                .isAfter(activeOperationTime.plusSeconds(Constants.MAX_OPERATION_TIME_BEFORE_RESUMING))) {
            luckyStackWorkerContext.setActiveOperationTime(LocalDateTime.now());
            profile = profileService.updateProfile(profileDTO);
            psfImage = referenceImageService.updateProcessing(profile, operations);
            luckyStackWorkerContext.setActiveOperationTime(null);
        } else {
            log.warn("Attempt to update image while another operation was in progress");
        }
        PSFImageDto psfImageDto = psfImage == null ? null : PSFImageDto.builder().imageData(Base64.getEncoder().encodeToString(psfImage)).build();
        return ResponseEntity.ok(UpdateProfileResponseDto.builder()
                .psfImage(psfImageDto)
                .profile(profile == null ? null : new ProfileDTO(profile))
                .build());
    }

    @PutMapping("/apply")
    public void applyProfile(@RequestBody ProfileDTO profile) throws IOException {
        String profileName = profile.getName();
        if (profileName != null) {
            luckyStackWorkerContext.setStatus(Constants.STATUS_WORKING);
            luckyStackWorkerContext.setSelectedRoi(
                    referenceImageService.getDisplayedImage().getRoi());
            luckyStackWorkerContext.setSelectedProfile(profileName);
            referenceImageService.writeProfile();
            luckyStackWorkerContext.setProfileBeingApplied(true);
        } else {
            log.warn("Attempt to apply profile while nothing was selected");
            luckyStackWorkerContext.setStatus(Constants.STATUS_IDLE);
        }
    }

    @GetMapping("/status")
    public StatusUpdateDTO getStatus() {
        log.info("getStatus called");
        return luckyStackWorkerContext.getStatusUpdateDTO();
    }

    @GetMapping("/version")
    public VersionDTO getLatestVersion() {
        log.info("getLatestVersion called");
        return referenceImageService.getLatestVersion(LocalDateTime.now());
    }

    @PutMapping("/scale")
    public ResponseDTO scale(@RequestBody ProfileDTO profileDTO) throws IOException {
        Profile profile = profileService.updateProfile(profileDTO);
        return referenceImageService.scale(profile);
    }

    @PutMapping("/stop")
    public void stopWorker() {
        luckyStackWorkerContext.setWorkerStopped(true);
    }

    @PutMapping("/exit")
    public void exit() {
        log.info("Exit called, ending application");
        Runtime.getRuntime().halt(0);
    }

    @GetMapping("/custom-psf")
    public SettingsDTO loadCustomPSF() throws IOException {
        log.info("loadCustomPSF called");
        return psfService.loadCustomPSF();
    }
}
