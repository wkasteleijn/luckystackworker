package nl.wilcokas.luckystackworker.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
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

import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.RoiListener;
import ij.gui.Toolbar;
import ij.io.Opener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.dto.Version;
import nl.wilcokas.luckystackworker.model.OperationEnum;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.model.Settings;
import nl.wilcokas.luckystackworker.repository.SettingsRepository;
import nl.wilcokas.luckystackworker.util.Operations;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
@Service
public class ReferenceImageService implements RoiListener, WindowListener, ComponentListener {

    private ImagePlus referenceImage;
    private ImagePlus processedImage;
    @Getter
    private boolean isLargeImage = false;

    @Getter
    private ImagePlus finalResultImage;

    private OperationEnum previousOperation;
    private String filePath;

    private boolean roiActive = false;
    private JFrame roiIndicatorFrame = null;
    private JTextField roiIndicatorTextField = null;

    private int zoomFactor = 0;

    private Image iconImage = new ImageIcon(getClass().getResource("/luckystackworker_icon.png")).getImage();

    private SettingsRepository settingsRepository;
    private HttpService httpService;
    private ProfileService profileService;

    public ReferenceImageService(SettingsRepository settingsRepository, HttpService httpService, ProfileService profileService) {
        this.settingsRepository = settingsRepository;
        this.httpService = httpService;
        this.profileService = profileService;
        createRoiIndicator();
    }

    public Profile selectReferenceImage(String filePath) throws IOException {
        JFrame frame = getParentFrame();
        JFileChooser jfc = getJFileChooser(filePath);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TIFF, PNG", "tif", "png");
        jfc.setFileFilter(filter);
        int returnValue = jfc.showOpenDialog(frame);
        frame.dispose();
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = jfc.getSelectedFile();
            String selectedFilePath = selectedFile.getAbsolutePath();
            if (validateSelectedFile(selectedFilePath)) {
                log.info("Image selected {} ", selectedFilePath);
                String fileNameNoExt = Util.getFilename(selectedFilePath)[0];
                Profile profile = Util.readProfile(fileNameNoExt);
                if (profile == null) {
                    String profileName = Util.deriveProfileFromImageName(selectedFilePath);
                    if (profileName == null) {
                        log.info("Profile not found for reference image, taking the default, {}", profileName);
                        profileName = getSettings().getDefaultProfile();
                    }
                    profile = profileService.findByName(profileName)
                            .orElseThrow(() -> new ResourceNotFoundException("Unknown profile!"));
                } else {
                    profileService.updateProfile(profile);
                    log.info("Profile file found, profile was loaded from there.");
                }
                this.isLargeImage = openReferenceImage(selectedFilePath, profile);

                final String rootFolder = Util.getFileDirectory(selectedFilePath);
                updateSettings(rootFolder, profile);
                profile.setLargeImage(this.isLargeImage);
                return profile;
            }
        }
        return new Profile();
    }

    public void updateProcessing(Profile profile) {
        final OperationEnum operation = profile.getOperation() == null ? null
                : OperationEnum.valueOf(profile.getOperation().toUpperCase());
        if (previousOperation == null || previousOperation != operation) {
            Util.copyInto(referenceImage, processedImage, finalResultImage.getRoi(), profile, false);
            if (Operations.isSharpenOperation(operation)) {
                Operations.applyAllOperationsExcept(processedImage, profile, operation, OperationEnum.DENOISEAMOUNT,
                        OperationEnum.DENOISERADIUS, OperationEnum.DENOISESIGMA, OperationEnum.DENOISEITERATIONS,
                        OperationEnum.SAVITZKYGOLAYAMOUNT, OperationEnum.SAVITZKYGOLAYITERATIONS, OperationEnum.SAVITZKYGOLAYSIZE);
            } else {
                Operations.applyAllOperationsExcept(processedImage, profile, operation);
            }
            previousOperation = operation;
        }
        Util.copyInto(processedImage, finalResultImage, finalResultImage.getRoi(), profile, true);
        setDefaultLayoutSettings(finalResultImage, finalResultImage.getWindow().getLocation());

        if (Operations.isSharpenOperation(operation)) {
            Operations.applySharpen(finalResultImage, profile);
            Operations.applyDenoise(finalResultImage, profile);
            Operations.applySavitzkyGolayDenoise(finalResultImage, profile);
        } else if (Operations.isDenoiseOperation(operation)) {
            Operations.applyDenoise(finalResultImage, profile);
        } else if (Operations.isSavitzkyGolayDenoiseOperation(operation)) {
            Operations.applySavitzkyGolayDenoise(finalResultImage, profile);
        } else if ((OperationEnum.CONTRAST == operation) || (OperationEnum.BRIGHTNESS == operation)
                || (OperationEnum.BACKGROUND == operation)) {
            Operations.applyBrightnessAndContrast(finalResultImage, profile, false);
        }

        // Exception for gamma, always apply last and only on the final result as it
        // messes up the sharpening.
        Operations.applyGammaAndRGBCorrections(finalResultImage, profile, false);

        Operations.applySaturation(finalResultImage, profile);
        finalResultImage.updateAndDraw();

        finalResultImage.setTitle(filePath);
    }

    public void saveReferenceImage(String path) throws IOException {
        String dir = Util.getFileDirectory(filePath);
        log.info("Saving image to folder {}", dir);
        String fileNameNoExt = Util.getFilename(path)[0];
        String finalPath = fileNameNoExt + "." + Constants.SUPPORTED_OUTPUT_FORMAT;
        Util.saveImage(finalResultImage, finalPath, Util.isPngRgbStack(finalResultImage, filePath), roiActive);
        log.info("Saved file to {}", finalPath);
        writeProfile(fileNameNoExt);
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

    public void updateSettings(String rootFolder, Profile profile) {
        log.info("Setting the root folder to {}", rootFolder);
        Settings settings = getSettings();
        settings.setRootFolder(rootFolder);
        settingsRepository.save(settings);
        LuckyStackWorkerContext.updateWorkerForRootFolder(rootFolder);
        profile.setRootFolder(rootFolder);
        String selectedProfile = profile.getName();
        if (selectedProfile != null) {
            LuckyStackWorkerContext.setSelectedProfile(selectedProfile);
        }
    }

    public void zoomIn() {
        IJ.run(finalResultImage, "In [+]", null);
        zoomFactor++;
    }

    public void zoomOut() {
        IJ.run(finalResultImage, "Out [-]", null);
        zoomFactor--;
    }

    public void crop() {
        if (!roiActive) {
            int width = finalResultImage.getWidth() / 2;
            int height = finalResultImage.getHeight() / 2;
            int x = (finalResultImage.getWidth() - width) / 2;
            int y = (finalResultImage.getHeight() - height) / 2;
            finalResultImage.setRoi(x, y, width, height);
            new Toolbar().setTool(Toolbar.RECTANGLE);
            LuckyStackWorkerContext.setSelectedRoi(finalResultImage.getRoi());
            roiActive = true;
            showRoiIndicator();
        } else {
            roiActive = false;
            finalResultImage.deleteRoi();
            new Toolbar().setTool(Toolbar.HAND);
            LuckyStackWorkerContext.setSelectedRoi(null);
            hideRoiIndicator();
        }
    }

    public Settings getSettings() {
        return settingsRepository.findAll().iterator().next();
    }

    public void writeProfile() throws IOException {
        String fileNameNoExt = Util.getFilename(filePath)[0];
        writeProfile(fileNameNoExt);
    }

    public void writeProfile(String fileNameNoExt) throws IOException {
        String profileName = LuckyStackWorkerContext.getSelectedProfile();
        if (profileName != null) {
            Profile profile = profileService.findByName(profileName)
                    .orElseThrow(() -> new ResourceNotFoundException(String.format("Unknown profile %s", profileName)));
            Util.writeProfile(profile, fileNameNoExt);
        } else {
            log.warn("Profile not saved, could not find the selected profile for file {}", fileNameNoExt);
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public Version getLatestVersion(LocalDateTime currentDate) {
        Settings settings = getSettings();
        String latestKnowVersion = settings.getLatestKnownVersion();
        if (settings.getLatestKnownVersionChecked() == null || currentDate
                .isAfter(settings.getLatestKnownVersionChecked().plusDays(Constants.VERSION_REQUEST_FREQUENCY))) {
            String latestVersionFromSite = requestLatestVersion();
            if (latestVersionFromSite != null) {
                settings.setLatestKnownVersion(latestVersionFromSite);
            }
            settings.setLatestKnownVersionChecked(currentDate);
            settingsRepository.save(settings);

            if (latestVersionFromSite != null && !latestVersionFromSite.equals(latestKnowVersion)) {
                return Version.builder().latestVersion(latestVersionFromSite).isNewVersion(true).build();
            }
        }
        return Version.builder().latestVersion(latestKnowVersion).isNewVersion(false).build();
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
        roiIndicatorFrame.setVisible(false);
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        roiIndicatorFrame.setVisible(true);
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

    private void createRoiIndicator() {
        roiIndicatorFrame = new JFrame();
        roiIndicatorFrame.setType(javax.swing.JFrame.Type.UTILITY);
        roiIndicatorFrame.setAlwaysOnTop(true);
        roiIndicatorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        roiIndicatorFrame.getContentPane().setBackground(Color.BLACK);
        roiIndicatorFrame.setUndecorated(true);
        roiIndicatorFrame.setSize(64, 24);
        roiIndicatorTextField = new JTextField();
        roiIndicatorTextField.setBackground(Color.BLACK);
        roiIndicatorTextField.setForeground(Color.LIGHT_GRAY);
        roiIndicatorTextField.setEditable(false);
        roiIndicatorTextField.setBorder(null);
        roiIndicatorTextField.setSelectedTextColor(Color.LIGHT_GRAY);
        roiIndicatorFrame.add(roiIndicatorTextField);
    }

    private void showRoiIndicator() {
        setRoiIndicatorLocation();
        updateRoiText();
        roiIndicatorFrame.setVisible(true);
    }

    private void updateRoiText() {
        if (finalResultImage.getRoi() != null) {
            roiIndicatorTextField.setText(((int) finalResultImage.getRoi().getFloatWidth()) + " x "
                    + ((int) finalResultImage.getRoi().getFloatHeight()));
        }

    }

    private void setRoiIndicatorLocation() {
        Window parentWindow = finalResultImage.getWindow();
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

    private boolean openReferenceImage(String filePath, Profile profile) throws IOException {
        this.filePath = filePath;
        finalResultImage = new Opener().openImage(Util.getIJFileFormat(this.filePath));
        boolean isLargeImage = false;
        if (finalResultImage != null) {
            if (Util.isPngRgbStack(finalResultImage, filePath)) {
                finalResultImage = Util.fixNonTiffOpeningSettings(finalResultImage);
            }
            Operations.correctExposure(finalResultImage);
            log.info("Opened final result image image with id {}", finalResultImage.getID());
            finalResultImage.show(filePath);
            isLargeImage = setZoom(finalResultImage, false);
            setDefaultLayoutSettings(finalResultImage,
                    new Point(Constants.DEFAULT_WINDOWS_POSITION_X, Constants.DEFAULT_WINDOWS_POSITION_Y));
            zoomFactor = 0;
            roiActive = false;

            processedImage = finalResultImage.duplicate();
            log.info("Opened duplicate image with id {}", processedImage.getID());
            processedImage.show();
            processedImage.getWindow().setVisible(false);

            referenceImage = processedImage.duplicate();
            referenceImage.show();
            referenceImage.getWindow().setVisible(false);
            log.info("Opened reference image image with id {}", referenceImage.getID());


            if (profile != null) {
                updateProcessing(profile);
            }

            finalResultImage.setTitle(this.filePath);
        }
        return isLargeImage;
    }

    private void setDefaultLayoutSettings(ImagePlus image, Point location) {
        image.setColor(Color.BLACK);
        image.setBorderColor(Color.BLACK);
        ImageWindow window = image.getWindow();
        window.setIconImage(iconImage);
        if (location != null) {
            window.setLocation(location);
        }
        new Toolbar().setTool(Toolbar.HAND);
        image.getRoi().addRoiListener(this);
        image.getWindow().addWindowListener(this);
        image.getWindow().addComponentListener(this);
    }

    private boolean setZoom(ImagePlus image, boolean isUpdate) {
        boolean isLargeImage = image.getWidth() > Constants.MAX_WINDOW_SIZE;
        if (!isUpdate && isLargeImage) {
            // zoomOut(); // ImageJ already does some zoomout if window is too large, even
            // though not enough.
        } else if (isUpdate) {
            if (zoomFactor < 0) {
                for (int i = 0; i < Math.abs(zoomFactor); i++) {
                    IJ.run(finalResultImage, "Out [-]", null);
                }
            } else if (zoomFactor > 0) {
                for (int i = 0; i < zoomFactor; i++) {
                    IJ.run(finalResultImage, "In [+]", null);
                }
            }
        }
        return isLargeImage;
    }

    private boolean validateSelectedFile(String path) {
        String extension = Util.getFilename(path)[1].toLowerCase();
        if (!getSettings().getExtensions().contains(extension)) {
            JOptionPane.showMessageDialog(getParentFrame(),
                    String.format(
                            "The selected file with extension %s is not supported. %nYou can only open 16-bit RGB and Gray PNG and TIFF images.",
                            extension));
            return false;
        }
        return true;
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
