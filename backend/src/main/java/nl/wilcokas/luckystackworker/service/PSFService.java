package nl.wilcokas.luckystackworker.service;

import static java.util.Collections.*;
import static nl.wilcokas.luckystackworker.constants.Constants.PSF_SIZE;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.Opener;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.dto.SettingsDTO;
import nl.wilcokas.luckystackworker.model.PSFType;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PSFService {

    private final ReferenceImageService referenceImageService;
    private final SettingsService settingsService;
    private final ProfileService profileService;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    public SettingsDTO loadCustomPSF() throws IOException {
        JFrame frame = referenceImageService.getParentFrame();
        JFileChooser jfc = referenceImageService.getJFileChooser(settingsService.getRootFolder());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TIF, TIFF, PNG", "tif", "tiff", "png");
        jfc.setFileFilter(filter);
        int returnValue = referenceImageService.getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String selectedFilePath = selectedFile.getAbsolutePath();
            String profileName = luckyStackWorkerContext.getSelectedProfile();
            Profile profile = profileService.findByName(profileName).orElse(null);
            if (profile != null) {
                String filePath = LswFileUtil.getIJFileFormat(selectedFilePath);
                ImagePlus psf = new Opener().openImage(filePath);
                if (psf.getStack().getSize() < 3) {
                    psf = LswImageProcessingUtil.create16BitRGBImage(
                            filePath, LswImageProcessingUtil.getImageLayers(psf), true, true, true);
                }
                if (psf.getWidth() > PSF_SIZE && psf.getHeight() > PSF_SIZE) {
                    log.warn(
                            "Uploaded PSF is too large ({} X {}), cropping to {} X {}",
                            psf.getWidth(),
                            psf.getHeight(),
                            PSF_SIZE,
                            PSF_SIZE);
                    int xOffset = (psf.getWidth() - PSF_SIZE) / 2;
                    int yOffset = (psf.getHeight() - PSF_SIZE) / 2;
                    psf = LswImageProcessingUtil.crop(psf, new Roi(xOffset, yOffset, PSF_SIZE, PSF_SIZE), filePath);
                } else if (psf.getWidth() < PSF_SIZE && psf.getHeight() < PSF_SIZE) {
                    log.warn(
                            "Uploaded PSF is too small ({} X {}), expanding it to {} X {}",
                            psf.getWidth(),
                            psf.getHeight(),
                            PSF_SIZE,
                            PSF_SIZE);
                    psf = LswImageProcessingUtil.expand(psf, PSF_SIZE, PSF_SIZE);
                }
                LswFileUtil.savePSF(psf, profileName);
                byte[] psfImage = LswFileUtil.getWienerDeconvolutionPSFImage(profileName);
                profile.getPsf().setType(PSFType.CUSTOM);
                referenceImageService.updateProcessing(profile, emptyList());
                return SettingsDTO.builder()
                        .psfImage(Base64.getEncoder().encodeToString(psfImage))
                        .build();
            }
        }
        return null;
    }
}
