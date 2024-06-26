package nl.wilcokas.luckystackworker.service;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDateTime;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import ij.process.ColorProcessor;
import nl.wilcokas.luckystackworker.ij.LswImageViewer;
import nl.wilcokas.luckystackworker.ij.histogram.LswImageMetadata;
import nl.wilcokas.luckystackworker.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.RoiListener;
import ij.gui.Toolbar;
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

@Slf4j
@Service
public class ReferenceImageService implements RoiListener, WindowListener, ComponentListener {

    @Value("${spring.profiles.active}")
    private String activeOSProfile;

    @Getter
    private LswImageViewer displayedImage;

    @Getter
    private boolean isLargeImage = false;

    private ImagePlus finalResultImage;

    private LswImageMetadata imageMetadata;

    private boolean roiActive = false;
    private JFrame roiIndicatorFrame = null;
    private JTextField roiIndicatorTextField = null;

    private boolean showHistogram = true;

    private int zoomFactor = 0;

    private LswImageLayersDto unprocessedImageLayers;

    private static Image iconImage;

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

    public ReferenceImageService(final SettingsService settingsService, final HttpService httpService, final ProfileService profileService,
                                 final OperationService operationService, final GmicService gmicService) {
        this.settingsService = settingsService;
        this.httpService = httpService;
        this.profileService = profileService;
        this.operationService = operationService;
        createRoiIndicator();
    }

    public ResponseDTO scale(Profile profile) throws IOException, InterruptedException {
        this.isLargeImage = openReferenceImage(this.imageMetadata.getFilePath(), profile, LswFileUtil.getObjectDateTime(this.imageMetadata.getFilePath()));
        SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
        settingsDTO.setLargeImage(this.isLargeImage);
        LuckyStackWorkerContext.setSelectedProfile(profile.getName());
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
                    profile = profileService.findByName(profileName)
                            .orElseThrow(() -> new ProfileNotFoundException("Unknown profile!"));
                } else {
                    log.info("Profile file found, profile was loaded from there.");
                }
                LswImageProcessingUtil.setNonPersistentSettings(profile, scale, openImageMode);
                profileService.updateProfile(new ProfileDTO(profile));

                isLargeImage = openReferenceImage(selectedFilePath, profile, LswFileUtil.getObjectDateTime(selectedFilePath));

                final String rootFolder = LswFileUtil.getFileDirectory(selectedFilePath);
                SettingsDTO settingsDTO = new SettingsDTO(updateSettingsForRootFolder(rootFolder));
                settingsDTO.setLargeImage(isLargeImage);
                LuckyStackWorkerContext.setSelectedProfile(profile.getName());
                return new ResponseDTO(new ProfileDTO(profile), settingsDTO);
            }
        }
        return null;
    }

    public void updateProcessing(Profile profile, String operationValue) throws IOException, InterruptedException {
        LswImageProcessingUtil.copyLayers(unprocessedImageLayers, finalResultImage, true, true, true);
        operationService.applyAllOperations(finalResultImage, displayedImage, profile);
        finalResultImage.updateAndDraw();
        LswImageProcessingUtil.copyLayers(LswImageProcessingUtil.getImageLayers(finalResultImage), displayedImage, true, true, true);
        updateHistogramMetadata();
        displayedImage.updateMetadata(imageMetadata);
        displayedImage.updateAndDraw();
    }

    public void saveReferenceImage(String path, boolean asJpg, Profile profile) throws IOException {
        String pathNoExt = LswFileUtil.getPathWithoutExtension(path);
        String savePath = pathNoExt + "." + (asJpg ? "jpg" : Constants.DEFAULT_OUTPUT_FORMAT);
        log.info("Saving image to  {}", savePath);
        LswFileUtil.saveImage(finalResultImage, null, savePath, LswFileUtil.isPngRgbStack(finalResultImage, imageMetadata.getFilePath()) || profile.getScale() > 1.0, roiActive, asJpg,
                false);
        writeProfile(pathNoExt);
    }

    public MyFileChooser getJFileChooser(String path) {
        MyFileChooser jfc = new MyFileChooser(path);
        jfc.requestFocus();
        return jfc;
    }

    public JFrame getParentFrame() {
        final JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.requestFocus();
        frame.setIconImage(iconImage);
        return frame;
    }

    public Settings updateSettingsForRootFolder(String rootFolder) {
        log.info("Setting the root folder to {}", rootFolder);
        Settings settings = settingsService.getSettings();
        settings.setRootFolder(rootFolder);
        settingsService.saveSettings(settings);
        LuckyStackWorkerContext.setRootFolderIsSelected();
        return settings;
    }

    public void zoomIn() {
        if (zoomFactor <= 5) {
            displayedImage.getImageWindow().zoomIn();
            zoomFactor++;
        }
    }

    public void zoomOut() {
        if (zoomFactor >= -3) {
            displayedImage.getImageWindow().zoomOut();
            zoomFactor--;
        }
    }

    public void minimize() {
        displayedImage.getImageWindow().setState(Frame.ICONIFIED);
    }

    public void maximize() {
        displayedImage.getImageWindow().setState(Frame.NORMAL);
    }

    public void crop() {
        if (!roiActive) {
            int width = displayedImage.getWidth() / 2;
            int height = displayedImage.getHeight() / 2;
            int x = (displayedImage.getWidth() - width) / 2;
            int y = (displayedImage.getHeight() - height) / 2;
            displayedImage.setRoi(x, y, width, height);
            new Toolbar().setTool(Toolbar.RECTANGLE);
            LuckyStackWorkerContext.setSelectedRoi(displayedImage.getRoi());
            roiActive = true;
            showRoiIndicator();
        } else {
            roiActive = false;
            displayedImage.deleteRoi();
            new Toolbar().setTool(Toolbar.HAND);
            LuckyStackWorkerContext.setSelectedRoi(null);
            hideRoiIndicator();
        }
    }

    public void histogram() {
        if (!showHistogram) {

            // TODO: update the histogram

            showHistogram = true;
        } else {
            showHistogram = false;
        }
    }

    public void night(boolean on) {
        if (showHistogram) {
            if (on) {
                // TODO: update the histogram color to RED
            } else {
                // TODO: update the histogram color to GREEN
            }
        }
    }

    public void writeProfile() throws IOException {
        String fileNameNoExt = LswFileUtil.getPathWithoutExtension(imageMetadata.getFilePath());
        writeProfile(fileNameNoExt);
    }

    public void writeProfile(String pathNoExt) throws IOException {
        String profileName = LuckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            Profile profile = profileService.findByName(profileName)
                    .orElseThrow(() -> new ProfileNotFoundException(String.format("Unknown profile %s", profileName)));
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
        if (settings.getLatestKnownVersionChecked() == null || currentDate
                .isAfter(settings.getLatestKnownVersionChecked().plusDays(Constants.VERSION_REQUEST_FREQUENCY))) {
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
            returnValue = jfc.showSaveDialog(frame);
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
        updateRoiText();
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        roiIndicatorFrame.setVisible(false);
    }

    @Override
    public void windowIconified(WindowEvent e) {
        if (roiActive) {
            roiIndicatorFrame.setVisible(false);
        }
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        if (roiActive) {
            roiIndicatorFrame.setVisible(true);
        }
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
        setRoiIndicatorLocation();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        setRoiIndicatorLocation();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        roiIndicatorFrame.setVisible(true);
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        roiIndicatorFrame.setVisible(false);
    }

    public void updateHistogramMetadata() {
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
    }

    private void setImageMetadata(final String filePath, final Profile profile, final LocalDateTime dateTime) {
        this.imageMetadata = LswImageMetadata.builder()
                .filePath(filePath)
                .name(LswUtil.getFullObjectName(profile.getName()))
                .width(finalResultImage.getWidth())
                .height(finalResultImage.getHeight())
                .time(dateTime)
                .build();
    }

    private void createRoiIndicator() {
        roiIndicatorFrame = new JFrame();
        roiIndicatorFrame.setType(javax.swing.JFrame.Type.UTILITY);
        roiIndicatorFrame.setAlwaysOnTop(true);
        roiIndicatorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        roiIndicatorFrame.getContentPane().setBackground(Color.BLACK);
        roiIndicatorFrame.setUndecorated(true);
        roiIndicatorFrame.setSize(72, 24);
        roiIndicatorTextField = new JTextField();
        roiIndicatorTextField.setBackground(Color.BLACK);
        roiIndicatorTextField.setForeground(Color.LIGHT_GRAY);
        roiIndicatorTextField.setEditable(false);
        roiIndicatorTextField.setBorder(null);
        roiIndicatorTextField.setSelectedTextColor(Color.LIGHT_GRAY);
        roiIndicatorFrame.add(roiIndicatorTextField);
    }

    private int getHistogramMax(int[] histogramValues) {
        int max = 1;
        for (int i = 0; i < histogramValues.length; i++) {
            if (histogramValues[i] > max) {
                max = histogramValues[i];
            }
        }
        return max;
    }

    private void showRoiIndicator() {
        setRoiIndicatorLocation();
        updateRoiText();
        roiIndicatorFrame.setVisible(true);
    }

    private void updateRoiText() {
        if (displayedImage.getRoi() != null) {
            roiIndicatorTextField.setText(((int) displayedImage.getRoi().getFloatWidth()) + " x " + ((int) displayedImage.getRoi().getFloatHeight()));
        }

    }

    private void setRoiIndicatorLocation() {
        Window parentWindow = displayedImage.getWindow();
        Point location = parentWindow.getLocation();
        roiIndicatorFrame.setLocation((int) location.getX() + 16, (int) (location.getY() + 32));
    }

    private void hideRoiIndicator() {
        roiIndicatorFrame.setVisible(false);
        roiActive = false;
    }

    private String requestLatestVersion() {

        // Retrieve version document
        String result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_1_1, Constants.VERSION_URL,
                Constants.VERSION_REQUEST_TIMEOUT);
        if (result == null) {
            log.warn("HTTP1.1 request for latest version failed, trying HTTP/2..");
            result = httpService.sendHttpGetRequest(HttpClient.Version.HTTP_2, Constants.VERSION_URL,
                    Constants.VERSION_REQUEST_TIMEOUT);
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
        finalResultImage = LswFileUtil.openImage(filePath, profile.getOpenImageMode(), finalResultImage, unprocessedImageLayers, profile.getScale(), img -> operationService.scaleImage(img, profile.getScale()));
        boolean largeImage = false;
        if (finalResultImage != null) {
            if (!LswFileUtil.validateImageFormat(finalResultImage, getParentFrame(), activeOSProfile)) {
                return false;
            }
            setImageMetadata(filePath, profile, dateTime);
            operationService.correctExposure(finalResultImage);
            unprocessedImageLayers = LswImageProcessingUtil.getImageLayers(finalResultImage);

            log.info("Opened final result image image with id {}", finalResultImage.getID());

            // Display the image in a seperate color image window (8-bit RGB color).
            displayedImage = createColorImageFrom(finalResultImage, unprocessedImageLayers, LswFileUtil.getImageName(LswFileUtil.getIJFileFormat(imageMetadata.getFilePath())));
            if (LswFileUtil.isPng(finalResultImage, imageMetadata.getFilePath())) {
                finalResultImage = LswFileUtil.fixNonTiffOpeningSettings(finalResultImage);
            }
            operationService.correctExposure(displayedImage);
            largeImage = displayedImage.getWidth() > Constants.MAX_WINDOW_SIZE;

            setDefaultLayoutSettings(displayedImage, new Point(Constants.DEFAULT_WINDOWS_POSITION_X, Constants.DEFAULT_WINDOWS_POSITION_Y));
            zoomFactor = 0;
            roiActive = false;

            log.info("Opened reference image image with id {}", displayedImage.getID());

            if (profile != null) {
                updateProcessing(profile, null);
            }
        }
        return largeImage;
    }

    private void setDefaultLayoutSettings(LswImageViewer image, Point location) {
        image.setColor(Color.BLACK);
        image.setBorderColor(Color.BLACK);
        image.show(imageMetadata.getFilePath());
        ImageWindow window = image.getWindow();
        window.setIconImage(iconImage);
        if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile)) {
            Taskbar.getTaskbar().setIconImage(iconImage);
        }
        if (location != null) {
            window.setLocation(location);
        }
        new Toolbar().setTool(Toolbar.HAND);
        image.getRoi().addRoiListener(this);
        image.getWindow().addWindowListener(this);
        image.getWindow().addComponentListener(this);
    }

    private boolean validateSelectedFile(String path) {
        String extension = LswFileUtil.getFilenameExtension(path);
        if (!settingsService.getSettings().getExtensions().contains(extension)) {
            JOptionPane.showMessageDialog(getParentFrame(),
                    String.format(
                            "The selected file with extension %s is not supported. %nYou can only open 16-bit RGB and Gray PNG and TIFF images.",
                            extension));
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
            dlg.setLocation(108, 128);
            return dlg;
        }
    }
}
