package nl.wilcokas.luckystackworker.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import ij.gui.*;
import ij.process.ColorProcessor;
import lombok.RequiredArgsConstructor;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.ij.LswImageWindow;
import nl.wilcokas.luckystackworker.ij.histogram.LswImageMetadata;
import nl.wilcokas.luckystackworker.model.ChannelEnum;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ij.ImagePlus;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.ProfileDTO;
import nl.wilcokas.luckystackworker.dto.ResponseDTO;
import nl.wilcokas.luckystackworker.dto.SettingsDTO;
import nl.wilcokas.luckystackworker.dto.VersionDTO;
import nl.wilcokas.luckystackworker.exceptions.ProfileNotFoundException;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.service.dto.LswImageLayersDto;

import static java.util.Collections.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceImageService implements RoiListener, WindowListener, ComponentListener {

    @Value("${spring.profiles.active}")
    private String activeOSProfile;

    @Getter
    private LswImageViewer displayedImage;

    @Getter
    private boolean isLargeImage = false;

    @Getter
    private boolean isMono = false;

    private ImagePlus finalResultImage;

    private LswImageMetadata imageMetadata;

    private boolean roiSwitched = false;

    private int zoomFactor = 0;

    private LswImageLayersDto unprocessedImageLayers;

    private int controllerLastKnownPositionX = -1;
    private int controllerLastKnownPositionY = -1;

    private ChannelEnum visibleChannel = ChannelEnum.RGB;

    private static Image iconImage;

    private Timer blinkClippedAreasTimer;
    private boolean isClippedAreasHighlighted = false;

    static {
        try {
            iconImage = new ImageIcon(new ClassPathResource("image_icon.png").getURL()).getImage();
        } catch (IOException e) {
            log.error("Error loading the icon png", e);
        }
    }

    private final SettingsService settingsService;
    private final HttpService httpService;
    private final ProfileService profileService;
    private final OperationService operationService;
    private final LuckyStackWorkerContext luckyStackWorkerContext;

    public ResponseDTO scale(Profile profile) throws IOException, InterruptedException {
        this.isLargeImage = openReferenceImage(this.imageMetadata.getFilePath(), profile, LswFileUtil.getObjectDateTime(this.imageMetadata.getFilePath()));
        SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
        settingsDTO.setLargeImage(this.isLargeImage);
        luckyStackWorkerContext.setSelectedProfile(profile.getName());
        return new ResponseDTO(new ProfileDTO(profile), settingsDTO);
    }

    public ResponseDTO selectReferenceImage(String filePath, double scale, String openImageMode) throws IOException, InterruptedException {
        JFrame frame = getParentFrame();
        JFileChooser jfc = getJFileChooser(filePath);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TIFF, PNG", "tif", "tiff", "png");
        jfc.setFileFilter(filter);
        int returnValue = getFilenameFromDialog(frame, jfc, false);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String selectedFilePath = selectedFile.getAbsolutePath();
            if (validateSelectedFile(selectedFilePath)) {
                log.info("Image selected {} ", selectedFilePath);
                String fileNameNoExt = LswFileUtil.getPathWithoutExtension(selectedFilePath);
                Profile profile = LswFileUtil.readProfile(fileNameNoExt);
                if (profile == null) {
                    String profileName = LswFileUtil.deriveProfileFromImageName(selectedFilePath);
                    if (profileName == null) {
                        log.info("Profile not found for reference image, taking the default, {}", profileName);
                        profileName = settingsService.getDefaultProfile();
                    }
                    profile = profileService.findByName(profileName).orElseThrow(() -> new ProfileNotFoundException("Unknown profile!"));
                } else {
                    log.info("Profile file found, profile was loaded from there.");
                }
                LswImageProcessingUtil.setNonPersistentSettings(profile, scale, openImageMode);
                profileService.updateProfile(new ProfileDTO(profile));

                isLargeImage = openReferenceImage(selectedFilePath, profile, LswFileUtil.getObjectDateTime(selectedFilePath));

                final String rootFolder = LswFileUtil.getFileDirectory(selectedFilePath);
                SettingsDTO settingsDTO = new SettingsDTO(updateSettingsForRootFolder(rootFolder));
                settingsDTO.setLargeImage(isLargeImage);
                settingsDTO.setZoomFactor(zoomFactor);
                byte[] psfImage = LswFileUtil.getWienerDeconvolutionPSFImage(profile.getName());
                if (psfImage != null) {
                    settingsDTO.setPsfImage(Base64.getEncoder().encodeToString(psfImage));
                }
                luckyStackWorkerContext.setSelectedProfile(profile.getName());
                return new ResponseDTO(new ProfileDTO(profile), settingsDTO);
            }
        }
        return null;
    }

    public byte[] updateProcessing(Profile profile, List<String> operationValues) throws IOException, InterruptedException {
        boolean includeRed = visibleChannel == ChannelEnum.RGB || visibleChannel == ChannelEnum.R;
        boolean includeGreen = visibleChannel == ChannelEnum.RGB || visibleChannel == ChannelEnum.G;
        boolean includeBlue = visibleChannel == ChannelEnum.RGB || visibleChannel == ChannelEnum.B;
        LswImageProcessingUtil.copyLayers(unprocessedImageLayers, finalResultImage, true, true, true);
        List<OperationEnum> operations = operationValues == null ? emptyList() : operationValues.stream().map(operationValue -> operationValue == null ? null : OperationEnum.valueOf(operationValue.toUpperCase())).toList();
        if (roiSwitched) {
            operations = emptyList();
            roiSwitched = false;
        }
        byte[] psf = operationService.applyAllOperations(finalResultImage, displayedImage, profile, operations, isMono);
        finalResultImage.updateAndDraw();
        LswImageProcessingUtil.copyLayers(LswImageProcessingUtil.getImageLayers(finalResultImage), displayedImage, includeRed, includeGreen, includeBlue);
        updateHistogramMetadata(profile);
        displayedImage.updateMetadata(imageMetadata);
        displayedImage.updateAndDraw();
        return psf;
    }

    public void saveReferenceImage(String path, boolean asJpg, Profile profile) throws IOException {
        String pathNoExt = LswFileUtil.getPathWithoutExtension(path);
        String savePath = pathNoExt + "." + (asJpg ? "jpg" : Constants.DEFAULT_OUTPUT_FORMAT);
        log.info("Saving image to  {}", savePath);
        ImagePlus savedImage = finalResultImage;
        if (luckyStackWorkerContext.isRoiActive()) {
            Roi roi = luckyStackWorkerContext.getSelectedRoi();
            savedImage = LswImageProcessingUtil.crop(finalResultImage, roi, path);
        }
        LswFileUtil.saveImage(savedImage, null, savePath, LswFileUtil.isPngRgbStack(savedImage, imageMetadata.getFilePath()) || profile.getScale() > 1.0, luckyStackWorkerContext.isRoiActive(), asJpg, false);
        writeProfile(pathNoExt);
    }

    public MyFileChooser getJFileChooser(String path) {
        MyFileChooser jfc = new MyFileChooser(path);
        jfc.requestFocus();
        return jfc;
    }

    public JFrame getParentFrame() {
        final JFrame frame = new JFrame();
        frame.setLocationByPlatform(true);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.requestFocus();
        frame.setIconImage(iconImage);
        return frame;
    }

    public Settings updateSettingsForRootFolder(String rootFolder) {
        log.info("Setting the root folder to {}", rootFolder);
        Settings settings = settingsService.getSettings();
        settings.setRootFolder(rootFolder);
        settingsService.saveSettings(settings);
        luckyStackWorkerContext.setRootFolderSelected(true);
        return settings;
    }

    public void zoomIn() {
        if (displayedImage != null) {
            zoomFactor++;
            imageMetadata.setZoomFactor(zoomFactor);
            displayedImage.getImageWindow().zoomIn(imageMetadata);
        }
    }

    public void zoomOut() {
        if (displayedImage != null) {
            zoomFactor--;
            imageMetadata.setZoomFactor(zoomFactor);
            displayedImage.getImageWindow().zoomOut(imageMetadata);
        }
    }

    public void minimize() {
        if (displayedImage != null) {
            displayedImage.getImageWindow().setState(Frame.ICONIFIED);
        }
    }

    public int maximize() {
        if (displayedImage != null) {
            double magnification = displayedImage.getImageWindow().showFullSizeImage();
            zoomFactor = convertMagnificationToZoomFactor(magnification);
            imageMetadata.setZoomFactor(zoomFactor);
            displayedImage.getImageWindow().updateMetadata(imageMetadata);
            return zoomFactor;
        }
        return 0;
    }

    public void restore() {
        if (displayedImage != null) {
            displayedImage.getImageWindow().setState(Frame.NORMAL);
        }
    }

    public void focus() {
        if (displayedImage != null) {
            displayedImage.getImageWindow().requestFocus();
        }
    }

    public void move(int x, int y) {
        log.info("Moving window to {}, {}", x, y);
        controllerLastKnownPositionX = x;
        controllerLastKnownPositionY = y;
    }

    public void roi() {
        operationService.clearCache();
        roiSwitched = true;
        if (!luckyStackWorkerContext.isRoiActive()) {
            if (luckyStackWorkerContext.getSelectedRoi() == null) {
                int width = displayedImage.getWidth() / 2;
                int height = displayedImage.getHeight() / 2;
                int x = (displayedImage.getWidth() - width) / 2;
                int y = (displayedImage.getHeight() - height) / 2;
                displayedImage.setRoi(x, y, width, height);
                luckyStackWorkerContext.setSelectedRoi(displayedImage.getRoi());
            } else {
                displayedImage.setRoi(luckyStackWorkerContext.getSelectedRoi());
            }
            new Toolbar().setTool(Toolbar.RECTANGLE);
            luckyStackWorkerContext.setRoiActive(true);
            updateRoiIndicator();
        } else {
            luckyStackWorkerContext.setRoiActive(false);
            displayedImage.deleteRoi();
            new Toolbar().setTool(Toolbar.HAND);
            hideRoiIndicator();
        }
    }

    public void writeProfile() throws IOException {
        String fileNameNoExt = LswFileUtil.getPathWithoutExtension(imageMetadata.getFilePath());
        writeProfile(fileNameNoExt);
    }

    public void writeProfile(String pathNoExt) throws IOException {
        String profileName = luckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            Profile profile = profileService.findByName(profileName).orElseThrow(() -> new ProfileNotFoundException(String.format("Unknown profile %s", profileName)));
            LswFileUtil.writeProfile(profile, pathNoExt);
        } else {
            log.warn("Profile not saved, could not find the selected profile for file {}", pathNoExt);
        }
    }

    public String getFilePath() {
        return imageMetadata.getFilePath();
    }

    public VersionDTO getLatestVersion(LocalDateTime currentDate) {
        Settings settings = settingsService.getSettings();
        String latestKnowVersion = settings.getLatestKnownVersion();
        if (settings.getLatestKnownVersionChecked() == null || currentDate.isAfter(settings.getLatestKnownVersionChecked().plusDays(Constants.VERSION_REQUEST_FREQUENCY))) {
            String latestVersionFromSite = requestLatestVersion();
            if (latestVersionFromSite != null) {
                settings.setLatestKnownVersion(latestVersionFromSite);
            }
            settings.setLatestKnownVersionChecked(currentDate);
            settingsService.saveSettings(settings);

            if (latestVersionFromSite != null && !latestVersionFromSite.equals(latestKnowVersion)) {
                return VersionDTO.builder().latestVersion(latestVersionFromSite).isNewVersion(true).build();
            }
        }
        return VersionDTO.builder().latestVersion(latestKnowVersion).isNewVersion(false).build();
    }

    public int getFilenameFromDialog(final JFrame frame, final JFileChooser jfc, boolean isSaveDialog) {
        return getFilenameFromDialog(frame, jfc, null, isSaveDialog);
    }

    public int getFilenameFromDialog(final JFrame frame, final JFileChooser jfc, String title, boolean isSaveDialog) {
        if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile) || Constants.SYSTEM_PROFILE_LINUX.equals(activeOSProfile)) {
            // Workaround for issue on macs, somehow needs to wait some milliseconds for the
            // frame to be initialized.
            LswUtil.waitMilliseconds(500);
        }
        int returnValue = 0;
        if (isSaveDialog) {
            boolean confirmed = false;
            do {
                returnValue = jfc.showSaveDialog(frame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = jfc.getSelectedFile();
                    if (fileToSave.exists()) {
                        if (confirmOverwrite()) {
                            confirmed = true;
                        }
                    } else {
                        confirmed = true;
                    }
                } else {
                    confirmed = true;
                }
            } while (!confirmed);
        } else if (title != null) {
            returnValue = jfc.showDialog(frame, title);
        } else {
            returnValue = jfc.showOpenDialog(frame);
        }
        frame.dispose();
        return returnValue;
    }

    @Override
    public void roiModified(ImagePlus imp, int id) {
        luckyStackWorkerContext.setSelectedRoi(imp.getRoi());
        updateRoiIndicator();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    public void updateHistogramMetadata(Profile profile) {
        int[] histogramValuesRed = finalResultImage.getStack().getProcessor(1).getHistogram(100);
        int[] histogramValuesGreen = finalResultImage.getStack().getProcessor(2).getHistogram(100);
        int[] histogramValuesBlue = finalResultImage.getStack().getProcessor(3).getHistogram(100);
        int redPercentage = 0;
        int greenPercentage = 0;
        int bluePercentage = 0;
        int redMax = getHistogramMax(histogramValuesRed);
        int greenMax = getHistogramMax(histogramValuesRed);
        int blueMax = getHistogramMax(histogramValuesRed);
        for (int i = 99; i >= 0; i--) {
            if ((histogramValuesRed[i] * 100) / redMax > 0) {
                redPercentage = i + 1;
                break;
            }
        }
        for (int i = 99; i >= 0; i--) {
            if ((histogramValuesGreen[i] * 100) / greenMax > 0) {
                greenPercentage = i + 1;
                break;
            }
        }
        for (int i = 99; i >= 0; i--) {
            if ((histogramValuesBlue[i] * 100) / blueMax > 0) {
                bluePercentage = i + 1;
                break;
            }
        }
        imageMetadata.setRed(redPercentage);
        imageMetadata.setGreen(greenPercentage);
        imageMetadata.setBlue(bluePercentage);
        imageMetadata.setLuminance((redPercentage + greenPercentage + bluePercentage) / 3);
        if (profile != null) {
            imageMetadata.setAngle(profile.getRotationAngle());
        }
    }

    public void nightMode(boolean on) {
        displayedImage.getImageWindow().nightMode(on);
    }

    public void showChannel(ChannelEnum channel) {
        boolean includeRed = channel == ChannelEnum.RGB || channel == ChannelEnum.R;
        boolean includeGreen = channel == ChannelEnum.RGB || channel == ChannelEnum.G;
        boolean includeBlue = channel == ChannelEnum.RGB || channel == ChannelEnum.B;
        LswImageProcessingUtil.copyLayers(LswImageProcessingUtil.getImageLayers(finalResultImage), displayedImage, includeRed, includeGreen, includeBlue);
        updateHistogramMetadata(null);
        visibleChannel = channel;
        imageMetadata.setChannel(visibleChannel);
        displayedImage.updateMetadata(imageMetadata);
        displayedImage.updateAndDraw();
    }

    public void blinkClippedAreas() {
        if (blinkClippedAreasTimer == null) {
            log.info("Enabling blinking on clipped areas");
            blinkClippedAreasTimer = new Timer();
            blinkClippedAreasTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isClippedAreasHighlighted) {
                        LswImageProcessingUtil.copyLayers(LswImageProcessingUtil.getImageLayers(finalResultImage), displayedImage, true, true, true);
                        isClippedAreasHighlighted = false;
                    } else {
                        int[] rgbPixels = (int[]) displayedImage.getProcessor().getPixels();
                        for (int i = 0; i < rgbPixels.length; i++) {
                            if (rgbPixels[i] == 0xFFFFFF) {
                                rgbPixels[i] = 0xFF0000;
                            }
                        }
                        isClippedAreasHighlighted = true;
                    }
                    displayedImage.repaintImage();
                }
            }, Constants.BLINK_CLIPPING_DELAY, Constants.BLINK_CLIPPING_DELAY);
        } else {
            log.info("Disabling blinking on clipped areas");
            LswImageProcessingUtil.copyLayers(LswImageProcessingUtil.getImageLayers(finalResultImage), displayedImage, true, true, true);
            displayedImage.repaintImage();
            blinkClippedAreasTimer.cancel();
            blinkClippedAreasTimer = null;
            isClippedAreasHighlighted = false;
        }
    }

    private boolean confirmOverwrite() {
        JOptionPane confirmation = new JOptionPane(
                "The file already exists.\nDo you want to replace it?",
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION);
        JDialog dialog = confirmation.createDialog(null, "Confirm Overwrite");
        dialog.setLocation(controllerLastKnownPositionX + 160, controllerLastKnownPositionY + 256);
        dialog.setVisible(true);
        if (((int) confirmation.getValue()) == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }
    
    private void setImageMetadata(final String filePath, final Profile profile, final LocalDateTime dateTime, final ImagePlus finalResultImage, double scale) {

        int currentWidth = finalResultImage.getWidth();
        int currentHeight = finalResultImage.getHeight();
        int originalWidth = (int) (finalResultImage.getWidth() / scale);
        int originalHeight = (int) (finalResultImage.getHeight() / scale);

        this.imageMetadata = LswImageMetadata.builder().filePath(filePath).name(LswUtil.getFullObjectName(profile.getName())).currentWidth(currentWidth).currentHeight(currentHeight).originalWidth(originalWidth).originalHeight(originalHeight).time(dateTime).channel(visibleChannel).angle((int) profile.getRotationAngle()).build();
    }

    private int getHistogramMax(int[] histogramValues) {
        int max = 1;
        for (int i = 16; i < histogramValues.length - 16; i++) {
            if (histogramValues[i] > max) {
                max = histogramValues[i];
            }
        }
        return max;
    }

    private void updateRoiIndicator() {
        int cropWidth = 0;
        int cropHeight = 0;
        if (displayedImage.getRoi() != null) {
            cropWidth = (int) displayedImage.getRoi().getFloatWidth();
            cropHeight = (int) displayedImage.getRoi().getFloatHeight();
        }
        imageMetadata.setCropWidth(cropWidth);
        imageMetadata.setCropHeight(cropHeight);
        displayedImage.getImageWindow().updateCrop(imageMetadata);
    }

    private void hideRoiIndicator() {
        imageMetadata.setCropWidth(0);
        imageMetadata.setCropHeight(0);
        displayedImage.getImageWindow().updateCrop(imageMetadata);
        luckyStackWorkerContext.setRoiActive(false);
    }

    private String requestLatestVersion() {

        // Retrieve version document
        String result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL, Constants.VERSION_REQUEST_TIMEOUT);
        if (result == null) {
            log.warn("HTTP1.1 request for latest version failed, trying HTTP/2..");
            result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_2, Constants.VERSION_URL, Constants.VERSION_REQUEST_TIMEOUT);
            if (result == null) {
                log.warn("HTTP/2 request for latest version failed as well");
            }
        }

        // Extract version
        String version = null;
        if (result != null) {
            version = getLatestVersion(result);
        }
        return version;
    }

    private String getLatestVersion(String htmlResponse) {
        int start = htmlResponse.indexOf(Constants.VERSION_URL_MARKER);
        if (start > 0) {
            int startVersionPos = start + Constants.VERSION_URL_MARKER.length();
            int endMarkerPos = htmlResponse.indexOf(Constants.VERSION_URL_ENDMARKER, startVersionPos);
            endMarkerPos = endMarkerPos < 0 ? startVersionPos : endMarkerPos; // robustness, don't fail if end marker
            // was missing
            String version = htmlResponse.substring(startVersionPos, endMarkerPos);
            if (validateVersion(version)) {
                log.info("Received valid version from server : {}", version);
                return version;
            } else {
                log.warn("Received an invalid version from the server");
            }
        }
        log.warn("Could not read the version from the server response to {}", Constants.VERSION_URL);
        return null;
    }

    private boolean validateVersion(String version) {
        if (version == null || version.length() == 0) {
            log.warn("Received an empty version from server : {}", version);
            return false;
        }
        if (version.length() > 10) { // 10.100.100 will never be reached :)
            log.warn("Received an invalid version nr from server : {}", version);
            return false;
        }
        return true;
    }

    private boolean openReferenceImage(String filePath, Profile profile, LocalDateTime dateTime) throws IOException, InterruptedException {
        if (displayedImage != null) {
            // Can only have 1 image open at a time.
            displayedImage.hide();
        }
        Pair<ImagePlus, Boolean> imageDetails = LswFileUtil.openImage(filePath, profile.getOpenImageMode(), finalResultImage, unprocessedImageLayers, profile.getScale(), img -> operationService.scaleImage(img, profile.getScale()));
        finalResultImage = imageDetails.getLeft();
        isMono = imageDetails.getRight();

        boolean largeImage = false;
        if (finalResultImage != null) {
            if (!LswFileUtil.validateImageFormat(finalResultImage, getParentFrame(), activeOSProfile)) {
                return false;
            }
            setImageMetadata(filePath, profile, dateTime, finalResultImage, profile.getScale());
            operationService.correctExposure(finalResultImage);
            operationService.clearCache();
            unprocessedImageLayers = LswImageProcessingUtil.getImageLayers(finalResultImage);

            log.info("Opened final result image image with id {}", finalResultImage.getID());

            // Display the image in a seperate color image window (8-bit RGB color).
            displayedImage = createColorImageFrom(finalResultImage, unprocessedImageLayers, LswFileUtil.getImageName(LswFileUtil.getIJFileFormat(imageMetadata.getFilePath())));
            if (LswFileUtil.isPng(finalResultImage, imageMetadata.getFilePath())) {
                finalResultImage = LswFileUtil.fixNonTiffOpeningSettings(finalResultImage);
            }
            operationService.correctExposure(displayedImage);
            largeImage = displayedImage.getWidth() * displayedImage.getHeight() > Constants.LARGE_WINDOW_SIZE;

            zoomFactor = setDefaultLayoutSettings(displayedImage);

            luckyStackWorkerContext.setRoiActive(false);
            luckyStackWorkerContext.setSelectedRoi(null);

            log.info("Opened reference image image with id {}", displayedImage.getID());

            if (profile != null) {
                updateProcessing(profile, emptyList());
            }
        }
        return largeImage;
    }

    private int setDefaultLayoutSettings(LswImageViewer image) {
        image.setColor(Color.BLACK);
        image.setBorderColor(Color.BLACK);
        image.show(imageMetadata.getFilePath());
        LswImageWindow window = (LswImageWindow) image.getWindow();
        window.setIconImage(iconImage);
        if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile)) {
            Taskbar.getTaskbar().setIconImage(iconImage);
        }
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        int positionX = Constants.DEFAULT_WINDOWS_POSITION_X;
        int positionY = Constants.DEFAULT_WINDOWS_POSITION_Y;
        if (controllerLastKnownPositionX >= 0 || controllerLastKnownPositionY >= 0) {
            positionX = controllerLastKnownPositionX + Constants.CONTROL_PANEL_WIDTH;
            if (Constants.SYSTEM_PROFILE_WINDOWS.equals(LswUtil.getActiveOSProfile())) {
                positionX += 8;
            }
            positionY = controllerLastKnownPositionY;
        }
        ImageCanvas canvas = window.getCanvas();
        int newZoomFactor = 0;
        if ((image.getWidth() + positionX) > screenDimension.getWidth() || (image.getHeight() + positionY) > screenDimension.getHeight()) {
            int remainingWidth = ((int) screenDimension.getWidth()) - positionX;
            int remainingHeight = ((int) screenDimension.getHeight()) - controllerLastKnownPositionY;
            canvas.zoomOut(remainingWidth, remainingHeight);
            newZoomFactor = convertMagnificationToZoomFactor(canvas.getMagnification());
            log.info("Adjusted window size to fit on screen. New zoom factor: {}", newZoomFactor);
            imageMetadata.setZoomFactor(newZoomFactor);
            window.updateMetadata(imageMetadata);
        } else {
            canvas.zoom100Percent();
        }
        new Toolbar().setTool(Toolbar.HAND);
        image.getRoi().addRoiListener(this);
        window.addWindowListener(this);
        window.addComponentListener(this);
        window.setLocation(positionX, positionY);
        return newZoomFactor;
    }

    private int convertMagnificationToZoomFactor(double magnification) {
        if (magnification < 1) {
            return -(int) ((1.0 - magnification) / 0.25);
        } else {
            return (int) ((magnification - 1) * 4.0);
        }
    }

    private boolean validateSelectedFile(String path) {
        String extension = LswFileUtil.getFilenameExtension(path);
        if (!settingsService.getSettings().getExtensions().contains(extension)) {
            JOptionPane.showMessageDialog(getParentFrame(), String.format("The selected file with extension %s is not supported. %nYou can only open 16-bit RGB and Gray PNG and TIFF images.", extension));
            return false;
        }
        return true;
    }

    private LswImageViewer createColorImageFrom(ImagePlus image, LswImageLayersDto layersDto, String title) {
        LswImageViewer singleLayerColorImage = new LswImageViewer(title, new ColorProcessor(image.getWidth(), image.getHeight()));
        LswImageProcessingUtil.convertLayersToColorImage(layersDto.getLayers(), singleLayerColorImage);
        return singleLayerColorImage;
    }

    final class MyFileChooser extends JFileChooser {

        MyFileChooser(String path) {
            super(path);
        }

        @Override
        protected JDialog createDialog(Component parent) throws HeadlessException {
            JDialog dlg = super.createDialog(parent);
            dlg.setLocation(controllerLastKnownPositionX + 8, controllerLastKnownPositionY + 36);
            return dlg;
        }
    }
}
