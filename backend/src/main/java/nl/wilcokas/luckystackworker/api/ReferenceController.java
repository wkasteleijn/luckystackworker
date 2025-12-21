package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.DeRotationDTO;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.dto.ResponseDTO;
import nl.wilcokas.luckystackworker.dto.SettingsDTO;
import nl.wilcokas.luckystackworker.exceptions.LswNotReadyException;
import nl.wilcokas.luckystackworker.model.ChannelEnum;
import nl.wilcokas.luckystackworker.model.DeRotation;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {"http://localhost:4200", "https://www.wilcokas.com"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

    private final SettingsRepository settingsService;
    private final ReferenceImageService referenceImageService;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    @GetMapping("/open")
    public ResponseDTO openReferenceImage(@RequestParam double scale, @RequestParam String openImageMode)
            throws IOException {
        return referenceImageService.selectReferenceImage(
                settingsService.getSettings().getRootFolder(), scale, openImageMode);
    }

    @GetMapping("/rootfolder")
    public SettingsDTO selectRootFolder() {
        JFrame frame = referenceImageService.getParentFrame();
        SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
        JFileChooser jfc = referenceImageService.getJFileChooser(settingsDTO.getRootFolder());
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = jfc.getSelectedFile();
            String rootFolder = LswFileUtil.getIJFileFormat(selectedFolder.getAbsolutePath());
            log.info("RootFolder selected {} ", rootFolder);
            settingsDTO = new SettingsDTO(referenceImageService.updateSettingsForRootFolder(rootFolder));
        }
        return settingsDTO;
    }

    @PutMapping("/save")
    public void saveReferenceImage(@RequestBody ProfileDTO profile) throws IOException {
        JFrame frame = referenceImageService.getParentFrame();
        JFileChooser jfc = referenceImageService.getJFileChooser(settingsService.getRootFolder());
        jfc.setFileFilter(new FileNameExtensionFilter("TIFF, JPG", "tif", "tiff", "jpg", "jpeg"));
        String fileNameNoExt = LswFileUtil.getFilename(referenceImageService.getFilePath());
        jfc.setSelectedFile(
                new File(fileNameNoExt + Constants.OUTPUT_POSTFIX_SAVE + "." + Constants.DEFAULT_OUTPUT_FORMAT));
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, "Save reference image", true);
        frame.dispose();
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            referenceImageService.saveReferenceImage(
                    selectedFile.getAbsolutePath(), asJpeg(selectedFile), new Profile(profile));
        }
    }

    @PutMapping("/zoomin")
    public void zoomIn() {
        referenceImageService.zoomIn();
    }

    @PutMapping("/zoomout")
    public void zoomOut() {
        referenceImageService.zoomOut();
    }

    @PutMapping("/minimize")
    public void minimize() {
        referenceImageService.minimize();
    }

    @PutMapping("/focus")
    public void focus() {
        referenceImageService.focus();
    }

    @PutMapping("/move")
    public void move(@RequestParam int x, @RequestParam int y) {
        referenceImageService.move(x, y);
    }

    @PutMapping("/maximize")
    public int maximize() {
        return referenceImageService.maximize();
    }

    @PutMapping("/restore")
    public void restore() {
        referenceImageService.restore();
    }

    @PutMapping("/roi")
    public void roi() {
        referenceImageService.roi();
    }

    @PutMapping("/night")
    public void nightMode(@RequestParam boolean on) {
        referenceImageService.nightMode(on);
    }

    @PutMapping("/realtime")
    public void realTimeChanged(@RequestBody boolean realtime) {
        if (realtime) {
            log.info("Re-enabling realtime processing");
            luckyStackWorkerContext.setRealTimeEnabled(true);
        } else {
            log.info("Disabling realtime processing");
            luckyStackWorkerContext.setRealTimeEnabled(false);
        }
    }

    @PutMapping("/channel")
    public void channelChanged(@RequestBody String channel) {
        referenceImageService.showChannel(ChannelEnum.valueOf(channel));
    }

    @GetMapping("/health")
    public SettingsDTO getHealthInfo() {
        Settings settings = settingsService.getSettings();
        if (settings == null) {
            log.warn("Initialization is still in progress of application startup failed");
            throw new LswNotReadyException("Initialization is still in progress of application startup failed");
        }
        return new SettingsDTO(settings);
    }

    @GetMapping("/open-for-derotation")
    public DeRotationDTO openDeRotationImages() {
        return new DeRotationDTO(referenceImageService.selectDerotationImages());
    }

    @PutMapping("/derotate")
    public void deRotate(@RequestBody DeRotationDTO deRotationDTO) {
        referenceImageService.derotate(new DeRotation(deRotationDTO));
    }

    @PutMapping("/stack")
    public void stack() {
        referenceImageService.stackImages();
    }

    private boolean asJpeg(File selectedFile) {
        String fileExtension = LswFileUtil.getFilenameExtension(selectedFile).toLowerCase();
        return "jpg".equals(fileExtension) || "jpeg".equals(fileExtension);
    }

    @PutMapping("/blink-clipped")
    public void blinkClippedAreas() {
        referenceImageService.blinkClippedAreas();
    }
}
