package nl.wilcokas.luckystackworker.api;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.ReferenceImageService;
import nl.wilcokas.luckystackworker.util.Util;

@CrossOrigin(origins = { "http://localhost:4200", "https://www.wilcokas.com" })
@RestController
@RequestMapping("/api/reference")
@Slf4j
public class ReferenceController {

    @Autowired
    private ReferenceImageService referenceImageService;

    @GetMapping("/open")
    public Profile openReferenceImage(@RequestParam String path) throws IOException {
        final String base64DecodedPath = new String(Base64.getDecoder().decode(path));
        return referenceImageService.selectReferenceImage(base64DecodedPath);
    }

    @GetMapping("/rootfolder")
    public Profile selectRootFolder() {
        JFrame frame = referenceImageService.getParentFrame();
        String rootFolder = LuckyStackWorkerContext.getWorkerProperties().get("inputFolder");
        JFileChooser jfc = referenceImageService
                .getJFileChooser(rootFolder);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        Profile profile = new Profile();
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = jfc.getSelectedFile();
            rootFolder = selectedFolder.getAbsolutePath();
            log.info("RootFolder selected {} ", rootFolder);
            referenceImageService.updateSettings(rootFolder, profile);
        } else {
            profile.setRootFolder(rootFolder);
        }
        return profile;
    }

    @PutMapping("/save")
    public void saveReferenceImage(@RequestBody Optional<String> path) throws IOException {
        JFrame frame = referenceImageService.getParentFrame();
        // Ignoring path received from frontend as it isn't used.
        String realPath = LuckyStackWorkerContext.getWorkerProperties().get("inputFolder");
        JFileChooser jfc = referenceImageService.getJFileChooser(realPath);
        jfc.setFileFilter(new FileNameExtensionFilter("TIFF, JPG", "tif", "jpg"));
        String fileNameNoExt = Util.getFilename(referenceImageService.getFilePath());
        jfc.setSelectedFile(
                new File(fileNameNoExt + Constants.OUTPUT_POSTFIX + "." + Constants.DEFAULT_OUTPUT_FORMAT));
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, "Save reference image", true);
        frame.dispose();
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            referenceImageService.saveReferenceImage(selectedFile.getAbsolutePath(), "jpg".equalsIgnoreCase(Util.getFilenameExtension(selectedFile)));
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
}
