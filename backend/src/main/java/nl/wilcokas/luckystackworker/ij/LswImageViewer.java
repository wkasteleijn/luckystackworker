package nl.wilcokas.luckystackworker.ij;

import ij.*;
import ij.process.ImageProcessor;
import nl.wilcokas.luckystackworker.util.LswUtil;

import java.awt.*;

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

    private void setActivated(boolean activated) {
        LswUtil.setPrivateField(this, LswImageViewer.class, "activated", activated);
    }

    private int getImageType() {
        return (Integer) LswUtil.getPrivateField(this, LswImageViewer.class, "imageType");
    }

    private int getDefault16bitDisplayRange() {
        return (Integer) LswUtil.getPrivateField(this, LswImageViewer.class, "default16bitDisplayRange");
    }
}
