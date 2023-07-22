package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.service.SettingsService;
import nl.wilcokas.luckystackworker.util.Util;

@CrossOrigin(origins = { "http://localhost:4200", "https://www.wilcokas.com" })
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

    private final SettingsService settingsService;
    private final ReferenceImageService referenceImageService;

    @GetMapping("/open")
    public ResponseDTO openReferenceImage(@RequestParam String path) throws IOException {
        final String base64DecodedPath = new String(Base64.getDecoder().decode(path));
        Pair<Profile, Boolean> imageData = referenceImageService.selectReferenceImage(base64DecodedPath);
        return new ResponseDTO(new ProfileDTO(imageData.getLeft()), SettingsDTO.builder().isLargeImage(imageData.getRight()).build());
    }

    @GetMapping("/rootfolder")
    public SettingsDTO selectRootFolder() {
        JFrame frame = referenceImageService.getParentFrame();
        SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
        JFileChooser jfc = referenceImageService
                .getJFileChooser(settingsDTO.getRootFolder());
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = jfc.getSelectedFile();
            String rootFolder = selectedFolder.getAbsolutePath();
            log.info("RootFolder selected {} ", rootFolder);
            referenceImageService.updateSettingsForRootFolder(rootFolder);
            settingsDTO.setRootFolder(rootFolder);
        }
        return settingsDTO;
    }

    @PutMapping("/save")
    public void saveReferenceImage(@RequestBody Optional<String> path) throws IOException {
        JFrame frame = referenceImageService.getParentFrame();
        // Ignoring path received from frontend as it isn't used.
        JFileChooser jfc = referenceImageService.getJFileChooser(settingsService.getRootFolder());
        jfc.setFileFilter(new FileNameExtensionFilter("TIFF, JPG", "tif", "tiff", "jpg", "jpeg"));
        String fileNameNoExt = Util.getFilename(referenceImageService.getFilePath());
        jfc.setSelectedFile(
                new File(fileNameNoExt + Constants.OUTPUT_POSTFIX_SAVE + "." + Constants.DEFAULT_OUTPUT_FORMAT));
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, "Save reference image", true);
        frame.dispose();
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            referenceImageService.saveReferenceImage(selectedFile.getAbsolutePath(), asJpeg(selectedFile));
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

    @PutMapping("/crop")
    public void crop() {
        referenceImageService.crop();
    }

    @PutMapping("/histogram")
    public void histogram() {
        referenceImageService.histogram();
    }

    @PutMapping("/night")
    public void nightMode(@RequestParam boolean on) {
        referenceImageService.night(on);
    }

    @PutMapping("/realtime")
    public void realTimeChanged(@RequestBody boolean realtime) {
        if (realtime) {
            log.info("Re-enabling realtime processing");
            LuckyStackWorkerContext.enableRealTimeEnabled();
        } else {
            log.info("Disabling realtime processing");
            LuckyStackWorkerContext.disableRealTimeEnabled();
        }
    }

    private boolean asJpeg(File selectedFile) {
        String fileExtension = Util.getFilenameExtension(selectedFile).toLowerCase();
        return "jpg".equals(fileExtension) || "jpeg".equals(fileExtension);
    }
}
