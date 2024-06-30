package nl.wilcokas.luckystackworker.ij;

import ij.*;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.ij.histogram.LswImageMetadata;
import nl.wilcokas.luckystackworker.util.LswUtil;

public class LswImageViewer extends ImagePlus {

    private LswImageWindow imageWindow;

    public LswImageViewer(String title, ImageProcessor ip) {
        super(title, ip);
    }

    /*
     * Override the ImagePlus.show() method so that we can create a custom ImageWindow (LswImageWindow),
     * for which we can set the window undecorated.
     */
    @Override
    public void show(String statusMessage) {
        win = null;
        img = getImage();
        setActivated(false);
        imageWindow = new LswImageWindow(this);
        win = imageWindow;
        if (roi != null) roi.setImage(this);
        if (getOverlay() != null && getCanvas() != null)
            getCanvas().setOverlay(getOverlay());
        IJ.showStatus(statusMessage);
        notifyListeners(OPENED);
    }

    public LswImageWindow getImageWindow() {
        return imageWindow;
    }

    @Override
    public synchronized void updateAndDraw() {
        super.updateAndDraw();
        imageWindow.repaint();
    }

    public void updateMetadata(final LswImageMetadata metadata) {
        imageWindow.updateMetadata(metadata);
    }

    private void setActivated(boolean activated) {
        LswUtil.setPrivateField(this, LswImageViewer.class, "activated", activated);
    }

}
