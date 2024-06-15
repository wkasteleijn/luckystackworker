package nl.wilcokas.luckystackworker.util;

import ij.*;
import ij.gui.*;
import ij.io.FileInfo;
import ij.macro.Interpreter;
import ij.process.ImageProcessor;

public class LswImageViewer extends ImagePlus {

    public LswImageViewer(String title, ImageProcessor ip) {
        super(title, ip);
    }

    /*
     * Override the ImagePlus.show() method so that we can create a custom ImageWindow (LswImageWindow),
     * for which we can set the window undecorated.
     */
    public void show(String statusMessage) {
        if (isVisible() || getTemporary())
            return;
        win = null;
        if ((IJ.isMacro() && getIJ()==null) || Interpreter.isBatchMode()) {
            ImagePlus imp = WindowManager.getCurrentImage();
            if (imp!=null) imp.saveRoi();
            WindowManager.setTempCurrentImage(this);
            Interpreter.addBatchModeImage(this);
            return;
        }
        img = getImage();
        if ((img!=null) && (width>=0) && (height>=0)) {
            setActivated(false);
            int stackSize = getStackSize();
            if (stackSize>1)
                win = new StackWindow(this);	// displays the window and (if macro) waits for window to be activated
            else if (getProperty(Plot.PROPERTY_KEY) != null)
                win = new PlotWindow(this, (Plot)(getProperty(Plot.PROPERTY_KEY)));
            else {
                win = new LswImageWindow(this);
            }
            if (roi!=null) roi.setImage(this);
            if (getOverlay()!=null && getCanvas()!=null)
                getCanvas().setOverlay(getOverlay());
            IJ.showStatus(statusMessage);
            if (IJ.isMacro() && stackSize==1) // for non-stacks, wait for window to be activated
                waitTillActivated();
            if (getImageType()==GRAY16 && getDefault16bitDisplayRange()!=0) {
                resetDisplayRange();
                updateAndDraw();
            }
            if (stackSize>1) {
                int c = getChannel();
                int z = getSlice();
                int t = getFrame();
                if (c>1 || z>1 || t>1)
                    setPosition(c, z, t);
            }
            if (setIJMenuBar)
                IJ.wait(25);
            notifyListeners(OPENED);
        }
    }

    private boolean getTemporary() {
        return (Boolean) LswUtil.getPrivateField(this, LswImageViewer.class, "temporary");
    }
    private ImageJ getIJ() {
        return (ImageJ) LswUtil.getPrivateField(this, LswImageViewer.class, "ij");
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
