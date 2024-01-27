package nl.wilcokas.luckystackworker.service;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.RoiListener;
import ij.gui.Toolbar;
import ij.io.Opener;
import ij.measure.Measurements;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
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
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswImageProcessingUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
@Service
public class ReferenceImageService implements RoiListener, WindowListener, ComponentListener {

    @Value("${spring.profiles.active}")
    private String activeOSProfile;

    @Getter
    private ImagePlus displayedImage;

    @Getter
    private boolean isLargeImage = false;

    private ImagePlus finalResultImage;

    private String filePath;

    private boolean roiActive = false;
    private JFrame roiIndicatorFrame = null;
    private JTextField roiIndicatorTextField = null;

    private boolean showHistogram = true;
    @Getter
    private PlotWindow plotWindow = null;
    private Plot histogramPlot = null;

    private int zoomFactor = 0;

    private LswImageLayersDto unprocessedImageLayers;

    private static Image iconImage;
    static {
        try {
            iconImage = new ImageIcon(new ClassPathResource("/luckystackworker_icon.png").getURL()).getImage();
        } catch (IOException e) {
            log.error("Error loading histogram stretch script");
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
        this.isLargeImage = openReferenceImage(null, profile);
        SettingsDTO settingsDTO = new SettingsDTO(settingsService.getSettings());
        settingsDTO.setLargeImage(this.isLargeImage);
        LuckyStackWorkerContext.setSelectedProfile(profile.getName());
        return new ResponseDTO(new ProfileDTO(profile), settingsDTO);
    }

    public ResponseDTO selectReferenceImage(String filePath, double scale) throws IOException, InterruptedException {
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
                LswImageProcessingUtil.setNonPersistentSettings(profile, scale);
                profileService.updateProfile(new ProfileDTO(profile));

                this.isLargeImage = openReferenceImage(selectedFilePath, profile);

                final String rootFolder = LswFileUtil.getFileDirectory(selectedFilePath);
                SettingsDTO settingsDTO = new SettingsDTO(updateSettingsForRootFolder(rootFolder));
                settingsDTO.setLargeImage(this.isLargeImage);
                LuckyStackWorkerContext.setSelectedProfile(profile.getName());
                return new ResponseDTO(new ProfileDTO(profile), settingsDTO);
            }
        }
        return null;
    }

    public void updateProcessing(Profile profile, String operationValue) throws IOException, InterruptedException {
        // Util.copyInto(displayedImage, finalResultImage, finalResultImage.getRoi(),
        // profile, true);
        // setDefaultLayoutSettings(finalResultImage,
        // finalResultImage.getWindow().getLocation());
        copyLayers(unprocessedImageLayers, this.finalResultImage);
        operationService.applyAllOperations(finalResultImage, profile);
        finalResultImage.updateAndDraw();
        copyLayers(getImageLayers(finalResultImage), displayedImage);
        displayedImage.updateAndDraw();
        if (showHistogram && plotWindow != null) {
            drawHistogram(false);
        }
    }

    public void saveReferenceImage(String path, boolean asJpg, Profile profile) throws IOException {
        String pathNoExt = LswFileUtil.getPathWithoutExtension(path);
        String savePath = pathNoExt + "." + (asJpg ? "jpg" : Constants.DEFAULT_OUTPUT_FORMAT);
        log.info("Saving image to  {}", savePath);
        LswFileUtil.saveImage(displayedImage, null, savePath, LswFileUtil.isPngRgbStack(displayedImage, filePath) || profile.getScale() > 1.0, roiActive, asJpg,
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
        IJ.run(displayedImage, "In [+]", null);
        zoomFactor++;
    }

    public void zoomOut() {
        IJ.run(displayedImage, "Out [-]", null);
        zoomFactor--;
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
            if (plotWindow != null) {
                plotWindow.setVisible(true);
            }
            createHistogram();
            showHistogram = true;
        } else {
            showHistogram = false;
            if (plotWindow != null) {
                plotWindow.setVisible(false);
            }
        }
    }

    public void night(boolean on) {
        if (showHistogram && plotWindow != null && plotWindow.isVisible()) {
            if (on) {
                histogramPlot.setColor(Color.RED, Color.RED);
            } else {
                histogramPlot.setColor(Color.GREEN, Color.GREEN);
            }
            this.drawHistogram(false);
        }
    }

    public void writeProfile() throws IOException {
        String fileNameNoExt = LswFileUtil.getPathWithoutExtension(filePath);
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
        return filePath;
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

    private void createHistogram() {
        createHistogramWindow();
        drawHistogram(true);
    }

    private void createHistogramWindow() {
        Point windowLocation = null;
        if (plotWindow != null && plotWindow.isVisible()) {
            plotWindow.setVisible(false);
            plotWindow.dispose();
            histogramPlot.dispose();
            windowLocation = plotWindow.getLocation();
        }
        histogramPlot = new Plot("Histogram", "Value", "Frequency");
        histogramPlot.setColor(Color.GREEN, Color.GREEN);
        histogramPlot.setBackgroundColor(Color.DARK_GRAY);
        // Hack Plot to force setting the processor since it was left null by the
        // constructor for mono images.
        if (this.finalResultImage.getStack().size() == 1) {
            LswUtil.setPrivateField(histogramPlot, Plot.class, "ip", displayedImage.getProcessor());
        }
        histogramPlot.setImagePlus(displayedImage);
        histogramPlot.setWindowSize(Constants.DEFAULT_HISTOGRAM_WINDOW_WIDTH, Constants.DEFAULT_HISTOGRAM_WINDOW_HEIGHT);
        histogramPlot.setXYLabels(null, null);
        histogramPlot.setXTicks(false);
        histogramPlot.setYTicks(false);
        histogramPlot.setAxisXLog(false);
        histogramPlot.setAxisYLog(false);
        histogramPlot.setXMinorTicks(false);
        histogramPlot.setYMinorTicks(false);
        plotWindow = histogramPlot.show();
        plotWindow.setBackground(Color.DARK_GRAY);
        plotWindow.setForeground(Color.BLACK);
        plotWindow.setIconImage(iconImage);
        plotWindow.getCanvas().setSize(Constants.DEFAULT_HISTOGRAM_WINDOW_WIDTH, Constants.DEFAULT_HISTOGRAM_WINDOW_HEIGHT + 48);
        plotWindow.setSize(Constants.DEFAULT_HISTOGRAM_WINDOW_WIDTH - 26, Constants.DEFAULT_HISTOGRAM_WINDOW_HEIGHT + 90);
        if (Constants.SYSTEM_PROFILE_MAC.equals(activeOSProfile)) {
            Taskbar.getTaskbar().setIconImage(iconImage);
        }
        plotWindow.setLocation(determineHistogramWindowLocation(windowLocation));
        plotWindow.remove(1);
        plotWindow.setResizable(false);
    }

    private Point determineHistogramWindowLocation(Point windowLocation) {
        Point imageWindowLocation = displayedImage.getWindow().getLocation();
        int imageWindowLocationX = (int) Math.round(imageWindowLocation.getX());
        int imageWindowLocationY = (int) Math.round(imageWindowLocation.getY());
        if (displayedImage.getHeight() < (Constants.DEFAULT_HISTOGRAM_WINDOW_HEIGHT * 3)) {
            return new Point(imageWindowLocationX, imageWindowLocationY + displayedImage.getHeight() + 74);
        } else if (displayedImage.getWidth() < Constants.MAX_IMAGE_WIDTH_HISTOGRAM) {
            return new Point(imageWindowLocationX + displayedImage.getWidth() + 10, imageWindowLocationY);
        } else {
            return new Point(imageWindowLocationX, imageWindowLocationY);
        }
    }

    private void drawHistogram(boolean isNew) {
        try {
            Pair<double[], double[]> histogram = getHistogram();
            double[] x = histogram.getLeft();
            double[] y = histogram.getRight();
            histogramPlot.setLimits(0, 65535, 0, getYLimit(y));
            if (isNew) {
                histogramPlot.add("bar", x, y);
            } else {
                histogramPlot.replace(0, "bar", x, y);
            }
            histogramPlot.update();
        } catch (Exception e) {
            log.error("Error drawing the histogram: ", e);
        }
    }

    private Pair<double[], double[]> getHistogram() {
        ImageStatistics stats = displayedImage.getStatistics(Measurements.AREA + Measurements.MEAN + Measurements.MODE + Measurements.MIN_MAX, 256);
        double[] y = stats.histogram();
        int n = y.length;
        double[] x = new double[n];
        double min = 0;
        for (int i = 0; i < n; i++) {
            x[i] = min + i * stats.binSize;
        }
        return Pair.of(x, y);
    }

    private double getYLimit(double[] y) {
        int n = y.length;
        int lowerBound = (int) Math.round(n * 0.1); // disregard the lowest and highest 10% of the histogram to determine maximum
        // displayed.
        int upperBound = (int) Math.round(n * 0.9);
        double max = 0.0;
        for (int i = lowerBound; i < upperBound; i++) {
            if (y[i] > max) {
                max = y[i];
            }
        }
        return max;
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

    private boolean openReferenceImage(String filePath, Profile profile) throws IOException, InterruptedException {
        this.filePath = filePath == null ? this.filePath : filePath;

        if (displayedImage != null) {
            // Can only have 1 image open at a time.
            displayedImage.hide();
        }

        finalResultImage = new Opener().openImage(LswFileUtil.getIJFileFormat(this.filePath));
        boolean largeImage = false;
        if (finalResultImage != null) {
            if (!LswFileUtil.validateImageFormat(finalResultImage, getParentFrame(), activeOSProfile)) {
                return false;
            }
            if (profile.getScale() > 1.0) {
                finalResultImage = operationService.scaleImage(finalResultImage, profile.getScale());
            }
            operationService.correctExposure(finalResultImage);
            unprocessedImageLayers = getImageLayers(finalResultImage);

            log.info("Opened final result image image with id {}", finalResultImage.getID());

            displayedImage = finalResultImage.duplicate();
            if (LswFileUtil.isPng(displayedImage, this.filePath)) {
                displayedImage = LswFileUtil.fixNonTiffOpeningSettings(displayedImage);
            }
            operationService.correctExposure(displayedImage);
            displayedImage.show(this.filePath);
            largeImage = setZoom(displayedImage, false);
            setDefaultLayoutSettings(displayedImage,
                    new Point(Constants.DEFAULT_WINDOWS_POSITION_X, Constants.DEFAULT_WINDOWS_POSITION_Y));
            zoomFactor = 0;
            roiActive = false;

            log.info("Opened reference image image with id {}", displayedImage.getID());

            if (profile != null) {
                updateProcessing(profile, null);
            }

            displayedImage.setTitle(this.filePath);

            if (showHistogram) {
                createHistogram();
            }
        }
        return largeImage;
    }

    private void setDefaultLayoutSettings(ImagePlus image, Point location) {
        image.setColor(Color.BLACK);
        image.setBorderColor(Color.BLACK);
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

    private boolean setZoom(ImagePlus image, boolean isUpdate) {
        boolean isLargeImage = image.getWidth() > Constants.MAX_WINDOW_SIZE;
        if (!isUpdate && isLargeImage) {
            // zoomOut(); // ImageJ already does some zoomout if window is too large, even
            // though not enough.
        } else if (isUpdate) {
            if (zoomFactor < 0) {
                for (int i = 0; i < Math.abs(zoomFactor); i++) {
                    IJ.run(displayedImage, "Out [-]", null);
                }
            } else if (zoomFactor > 0) {
                for (int i = 0; i < zoomFactor; i++) {
                    IJ.run(displayedImage, "In [+]", null);
                }
            }
        }
        return isLargeImage;
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

    private LswImageLayersDto getImageLayers(ImagePlus image) {
        ImageStack stack = image.getStack();
        short[][] newPixels = new short[3][stack.getProcessor(1).getPixelCount()];
        Executor executor = LswUtil.getParallelExecutor();
        for (int layer = 1; layer <= stack.size(); layer++) {
            int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(finalLayer);
                short[] pixels = (short[]) p.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    newPixels[finalLayer - 1][i] = pixels[i];
                }
            });
        }
        LswUtil.stopAndAwaitParallelExecutor(executor);
        return LswImageLayersDto.builder().layers(newPixels).count(stack.size()).build();
    }

    private void copyLayers(LswImageLayersDto layersDto, ImagePlus image) {
        ImageStack stack = image.getStack();
        Executor executor = LswUtil.getParallelExecutor();
        short[][] layers = layersDto.getLayers();
        for (int layer = 1; layer <= stack.size(); layer++) {
            final int finalLayer = layer;
            executor.execute(() -> {
                ImageProcessor p = stack.getProcessor(finalLayer);
                short[] pixels = (short[]) p.getPixels();
                for (int i = 0; i < pixels.length; i++) {
                    pixels[i] = layers[finalLayer - 1][i];
                }
            });
        }
        LswUtil.stopAndAwaitParallelExecutor(executor);
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
