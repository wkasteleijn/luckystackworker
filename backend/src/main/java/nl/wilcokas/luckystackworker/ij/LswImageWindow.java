package nl.wilcokas.luckystackworker.ij;

import ij.*;
import ij.gui.*;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.ij.histogram.LswImageMetadata;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Slf4j
public class LswImageWindow extends ImageWindow implements MouseMotionListener {

  private static final double SCALE = Prefs.getGuiScale();
  private static final Color TITLEBAR_COLOR = new Color(32, 32, 32);
  private static final Color TITLE_COLOR = new Color(100, 100, 100);
  private static final Color HISTOGRAM_COLOR_DAY = new Color(0x4c, 0xaf, 0x50);
  private static final Color HISTOGRAM_COLOR_NIGHT = new Color(0x9f, 0x58, 0x00);
  private static final Color HISTOGRAM_COLOR_WARN = new Color(174, 80, 80);
  private static final Color BORDER_COLOR = new Color(64, 64, 64);
  private static final Color BACKGROUND_COLOR = new Color(43, 43, 43);
  private static final int OFFSET_TOP = 32;
  private static final int HISTOGRAM_MARGIN_LEFT = 8;
  private static final int HISTOGRAM_MARGIN_TOP = 8;
  private static final int HISTOGRAM_HEIGHT = 56;
  private static final int textOffsetX = HISTOGRAM_HEIGHT * 2 + 12;
  private static final int textOffsetY = 49;
  private static final int textHeight = 12;

  private static Image maximizeImage;

  static {
    try {
      maximizeImage = new ImageIcon(new ClassPathResource("maximize.png").getURL()).getImage();
    } catch (IOException e) {
      log.error("Error loading the icon png", e);
    }
  }

  private int TEXT_GAP = 11;
  private ImagePlus image;
  private LswImageMetadata metadata;
  private double progressPercentage = 0;
  private int mouseX, mouseY;
  private Color histogramColor = HISTOGRAM_COLOR_DAY;
  private Color backgroundColor = BACKGROUND_COLOR;
  private boolean isMaximized = false;
  private Rectangle originalBounds;

  /*
   * Override the ImageWindow constructor so that we can create a custom ImageWindow (LswImageWindow),
   * for which we can set the window undecorated.
   */
  public LswImageWindow(ImagePlus imp) {
    super(imp.getTitle());
    this.image = imp;
    if (SCALE > 1.0) {
      TEXT_GAP = (int) (TEXT_GAP * SCALE);
      setTextGap(getCenterOnScreen() ? 0 : TEXT_GAP);
    }
    setForeground(Color.black);
    if (IJ.isLinux())
      setBackground(ImageJ.backgroundColor);
    else
      setBackground(Color.white);
    ij = IJ.getInstance();
    this.imp = imp;
    ic = new LswImageCanvas(imp);
    ic.setSize(imp.getWidth(), imp.getHeight());
    setNewCanvas(true);
    setLayout(new LswImageLayout(ic));
    add(ic);
    addFocusListener(this);
    addWindowListener(this);
    addWindowStateListener(this);
    addKeyListener(ij);
    setFocusTraversalKeysEnabled(false);
    addMouseWheelListener(this);
    this.addMouseMotionListener(this);
    setResizable(true);
    WindowManager.addWindow(this);
    imp.setWindow(this);
    setUndecorated(true);
    callSetLocationAndSize(false);
    if (ij != null && !IJ.isMacintosh()) {
      Image img = ij.getIconImage();
      if (img != null) try {
        setIconImage(img);
      } catch (Exception e) {
        log.error("Error setting icon image", e);
      }
    }
    show();
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    drawBorders(g);
    if (metadata != null) {
      paintHistogram(g);
      paintHistogramDetails(g);
      paintObjectDetails(g);
      paintImageDetails(g);
    }
  }

  public void updateMetadata(final LswImageMetadata metadata) {
    this.metadata = metadata;
    repaint();
  }

  public void updateProgress(final int progressPercentage) {
    this.progressPercentage = progressPercentage;
    paintProgressBar(getGraphics());
  }

  public void updateCrop(final LswImageMetadata metadata) {
    this.metadata = metadata;
    Graphics g = getGraphics();
    g.setColor(backgroundColor);
    g.fillRect(textOffsetX + 318, textOffsetY + textHeight * 4 - 11, 128, 12);
    g.setColor(histogramColor);
    g.drawString(getCropString(), textOffsetX + 318, textOffsetY + textHeight * 4);
  }

  public void zoomIn(final LswImageMetadata metadata) {
    this.metadata = metadata;
    int x = ic.screenX(image.getWidth() / 2);
    int y = ic.screenY(image.getHeight() / 2);
    ic.zoomIn(x, y);
    repaint();
    this.hasFocus();
  }

  public void zoomOut(final LswImageMetadata metadata) {
    this.metadata = metadata;
    int x = ic.screenX(image.getWidth() / 2);
    int y = ic.screenY(image.getHeight() / 2);
    ic.zoomOut(x, y);
    repaint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    synchronized (this) {
      if (mouseX == 0 || mouseY == 0) {
        mouseX = e.getX();
        mouseY = e.getY();
        return;
      }
      int dx = e.getX() - mouseX;
      int dy = e.getY() - mouseY;
      Point currentLocation = getLocation();
      setLocation(new Point(currentLocation.x + dx, currentLocation.y + dy));
    }
  }


  @Override
  public void mouseMoved(MouseEvent e) {
    super.mouseMoved(e.getX(), e.getY());
  }

  public void nightMode(boolean nightMode) {
    if (nightMode) {
      histogramColor = HISTOGRAM_COLOR_NIGHT;
      backgroundColor = Color.BLACK;
    } else {
      histogramColor = HISTOGRAM_COLOR_DAY;
      backgroundColor = BACKGROUND_COLOR;
    }
    repaint();
  }

  public double showFullSizeImage() {
    if (isMaximized) {
      isMaximized = false;
      setLocationAndSize(originalBounds.x, originalBounds.y, originalBounds.width, originalBounds.height);
      return getCanvas().getMagnification();
    } else {
      isMaximized = true;
      originalBounds = getBounds();
      this.setLocation(0, 0);
      getCanvas().zoom100Percent();
      return 1.0;
    }
  }

  private void drawBorders(Graphics g) {
    int thickness = 1;
    int width = getWidth();
    int height = getHeight();
    g.setColor(TITLEBAR_COLOR);
    g.fillRect(0, 0, width, OFFSET_TOP);
    g.setColor(BORDER_COLOR);
    g.fillRect(0, 0, width, thickness);
    g.fillRect(0, height - 1, width, thickness);
    g.fillRect(0, 0, thickness, height);
    g.fillRect(width - thickness, 0, thickness, height);
    g.setColor(backgroundColor);
    g.fillRect(0, 32, width, 72);
    int textWidth = g.getFontMetrics().stringWidth(image.getTitle());
    int x = (getWidth() - textWidth) / 2;
    g.setColor(TITLE_COLOR);
    g.drawString(image.getTitle(), x, 22);
  }

  private void paintHistogram(Graphics g) {
    ImageStatistics stats = image.getStatistics(Measurements.AREA + Measurements.MEAN + Measurements.MODE + Measurements.MIN_MAX, HISTOGRAM_HEIGHT);
    double[] luminanceHistogram = smoothen(stats.histogram());
    double maxValue = 1;
    for (int i = 8; i < luminanceHistogram.length - 8; i++) {
      if (luminanceHistogram[i] > maxValue) {
        maxValue = luminanceHistogram[i];
      }
    }
    for (int i = 0; i < luminanceHistogram.length; i++) {
      int intValue = (int) ((luminanceHistogram[i] / maxValue) * HISTOGRAM_HEIGHT);
      if (intValue > HISTOGRAM_HEIGHT) intValue = HISTOGRAM_HEIGHT;
      int blackSpace = HISTOGRAM_HEIGHT - intValue;
      g.setColor(backgroundColor);
      g.fillRect(HISTOGRAM_MARGIN_LEFT + i * 2, HISTOGRAM_MARGIN_TOP + OFFSET_TOP, 2, blackSpace);
      g.setColor(histogramColor);
      g.fillRect(HISTOGRAM_MARGIN_LEFT + i * 2, blackSpace + HISTOGRAM_MARGIN_TOP + OFFSET_TOP + 1, 2, HISTOGRAM_HEIGHT - blackSpace);
    }
    g.fillRect(HISTOGRAM_MARGIN_LEFT, HISTOGRAM_MARGIN_TOP + OFFSET_TOP, HISTOGRAM_HEIGHT * 2, 1);
    g.fillRect(HISTOGRAM_MARGIN_LEFT, HISTOGRAM_MARGIN_TOP + OFFSET_TOP + HISTOGRAM_HEIGHT, HISTOGRAM_HEIGHT * 2, 1);
    g.fillRect(HISTOGRAM_MARGIN_LEFT, HISTOGRAM_MARGIN_TOP + OFFSET_TOP, 1, HISTOGRAM_HEIGHT);
    g.fillRect(HISTOGRAM_HEIGHT * 2 - 1 + HISTOGRAM_MARGIN_LEFT, HISTOGRAM_MARGIN_TOP + OFFSET_TOP, 1, HISTOGRAM_HEIGHT);
  }

  private void paintHistogramDetails(Graphics g) {
    g.drawString("Histogram", textOffsetX, textOffsetY);

    setHistogramColor(g, metadata.getRed());
    g.drawString("Red :", textOffsetX, textOffsetY + textHeight);
    g.drawString(metadata.getRed() + "%", textOffsetX + 46, textOffsetY + textHeight);

    setHistogramColor(g, metadata.getGreen());
    g.drawString("Green :", textOffsetX, textOffsetY + textHeight * 2);
    g.drawString(metadata.getGreen() + "%", textOffsetX + 46, textOffsetY + textHeight * 2);

    setHistogramColor(g, metadata.getBlue());
    g.drawString("Blue :", textOffsetX, textOffsetY + textHeight * 3);
    g.drawString(metadata.getBlue() + "%", textOffsetX + 46, textOffsetY + textHeight * 3);

    setHistogramColor(g, metadata.getLuminance());
    g.drawString("Lum. :", textOffsetX, textOffsetY + textHeight * 4);
    g.drawString(metadata.getLuminance() + "%", textOffsetX + 46, textOffsetY + textHeight * 4);
  }

  private void paintObjectDetails(Graphics g) {
    g.setColor(histogramColor);
    g.drawString("Object", textOffsetX + 88, textOffsetY);

    g.drawString("Name :", textOffsetX + 88, textOffsetY + textHeight);
    g.drawString(metadata.getName(), textOffsetX + 144, textOffsetY + textHeight);

    g.drawString("Date :", textOffsetX + 88, textOffsetY + textHeight * 2);
    g.drawString(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(metadata.getTime()), textOffsetX + 144, textOffsetY + textHeight * 2);

    g.drawString("Time :", textOffsetX + 88, textOffsetY + textHeight * 3);
    g.drawString("%s UTC".formatted(DateTimeFormatter.ofPattern("HH:mm").format(metadata.getTime())), textOffsetX + 144, textOffsetY + textHeight * 3);

    g.drawString("Channel :", textOffsetX + 88, textOffsetY + textHeight * 4);
    g.drawString(getChannelDescription(), textOffsetX + 144, textOffsetY + textHeight * 4);
  }

  private void paintImageDetails(Graphics g) {
    g.drawString("Image", textOffsetX + 228, textOffsetY);

    g.drawString("Original size :", textOffsetX + 228, textOffsetY + textHeight);
    g.drawString("%s x %s".formatted(metadata.getOriginalWidth(), metadata.getOriginalHeight()), textOffsetX + 318, textOffsetY + textHeight);

    g.drawString("Current size :", textOffsetX + 228, textOffsetY + textHeight * 2);
    g.drawString("%s x %s".formatted(metadata.getCurrentWidth(), metadata.getCurrentHeight()), textOffsetX + 318, textOffsetY + textHeight * 2);

    g.drawString("Zoom :", textOffsetX + 228, textOffsetY + textHeight * 3);
    g.drawString(getZoomPercentage() + "%", textOffsetX + 318, textOffsetY + textHeight * 3);

    g.drawString("Crop :", textOffsetX + 228, textOffsetY + textHeight * 4);
    g.drawString(getCropString(), textOffsetX + 318, textOffsetY + textHeight * 4);
  }

  private int getZoomPercentage() {
    if (metadata.getZoomFactor() >= 0) {
      return 100 + (metadata.getZoomFactor() * 25);
    } else {
      return 100 - (Math.abs(metadata.getZoomFactor()) * 25);
    }
  }

  private String getChannelDescription() {
    return switch (metadata.getChannel()) {
      case "R" -> "Red";
      case "G" -> "Green";
      case "B" -> "Blue";
      default -> "RGB";
    };
  }

  private String getCropString() {
    String crop = "-";
    if (metadata.getCropWidth() > 0 && metadata.getCropHeight() > 0) {
      crop = "%s x %s".formatted(metadata.getCropWidth(), metadata.getCropHeight());
    }
    return crop;
  }

  private void paintProgressBar(Graphics g) {
    if (progressPercentage > 0) {
      g.setColor(histogramColor);
    } else {
      g.setColor(backgroundColor);
    }
    g.fillRect(0, HISTOGRAM_MARGIN_TOP + OFFSET_TOP + HISTOGRAM_HEIGHT + 6, (int) (image.getWidth() * (progressPercentage / 100D)), 4);
  }

  private void setHistogramColor(Graphics g, int histogramMax) {
    if (histogramMax == 100) {
      g.setColor(HISTOGRAM_COLOR_WARN);
    } else {
      g.setColor(histogramColor);
    }
  }

  private void setTextGap(int gap) {
    LswUtil.setPrivateField(this, ImageWindow.class, "textGap", gap);
  }

  private boolean getCenterOnScreen() {
    return (Boolean) LswUtil.getPrivateField(this, ImageWindow.class, "centerOnScreen");
  }

  private void setNewCanvas(boolean newCanvas) {
    LswUtil.setPrivateField(this, ImageWindow.class, "newCanvas", newCanvas);
  }

  private void callSetLocationAndSize(Boolean val) {
    try {
      Method privateMethod = Arrays.asList(ReflectionUtils.getAllDeclaredMethods(ImageWindow.class)).stream()
        .filter(m -> m.getName().equals("setLocationAndSize") && m.getParameterCount() == 1).findAny().get();
      privateMethod.setAccessible(true);
      privateMethod.invoke(this, val);
    } catch (IllegalAccessException | InvocationTargetException e) {
      log.warn("Could not call setLocationAndSize: ", e);
    }
  }

  private double[] smoothen(double[] histogram) {
    double[] newHistogram = new double[histogram.length];
    for (int i = 0; i < histogram.length; i++) {
      if (i == 0 || i == histogram.length - 1) {
        newHistogram[i] = histogram[i];
      } else {
        newHistogram[i] = (histogram[i] + histogram[i - 1] + histogram[i + 1]) / 3;
      }
    }
    return newHistogram;
  }
}
