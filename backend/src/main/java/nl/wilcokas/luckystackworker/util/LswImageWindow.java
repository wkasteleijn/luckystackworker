package nl.wilcokas.luckystackworker.util;

import ij.*;
import ij.gui.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ReflectionUtils;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class LswImageWindow extends ImageWindow {

    private final double SCALE = Prefs.getGuiScale();
    private int TEXT_GAP = 11;

    /*
     * Override the ImageWindow constructor so that we can create a custom ImageWindow (LswImageWindow),
     * for which we can set the window undecorated.
     */
    public LswImageWindow(ImagePlus imp) {
        super(imp.getTitle());
        if (SCALE > 1.0) {
            TEXT_GAP = (int) (TEXT_GAP * SCALE);
            setTextGap(getCenterOnScreen() ? 0 : TEXT_GAP);
        }
        if (Prefs.blackCanvas && getClass().getName().equals("ij.gui.ImageWindow")) {
            setForeground(Color.white);
            setBackground(Color.black);
        } else {
            setForeground(Color.black);
            if (IJ.isLinux())
                setBackground(ImageJ.backgroundColor);
            else
                setBackground(Color.white);
        }
        boolean openAsHyperStack = imp.getOpenAsHyperStack();
        ij = IJ.getInstance();
        this.imp = imp;
        if (ic == null) {
            ic = new ImageCanvas(imp);
            setNewCanvas(true);
        }
        this.ic = ic;
        ImageWindow previousWindow = imp.getWindow();
        setLayout(new ImageLayout(ic));
        add(ic);
        addFocusListener(this);
        addWindowListener(this);
        addWindowStateListener(this);
        addKeyListener(ij);
        setFocusTraversalKeysEnabled(false);
        addMouseWheelListener(this);
        setResizable(true);
        WindowManager.addWindow(this);
        imp.setWindow(this);
        setUndecorated(true);
        if (previousWindow != null) {
            if (getNewCanvas()) callSetLocationAndSize(false);
            Point loc = previousWindow.getLocation();
            setLocation(loc.x, loc.y);
            pack();
            show();
            if (ic.getMagnification() != 0.0)
                imp.setTitle(imp.getTitle());
            boolean unlocked = imp.lockSilently();
            boolean changes = imp.changes;
            imp.changes = false;
            previousWindow.close();
            imp.changes = changes;
            if (unlocked)
                imp.unlock();
            if (this.imp != null)
                this.imp.setOpenAsHyperStack(openAsHyperStack);
            WindowManager.setCurrentWindow(this);
        } else {
            callSetLocationAndSize(false);
            if (ij != null && !IJ.isMacintosh()) {
                Image img = ij.getIconImage();
                if (img != null) try {
                    setIconImage(img);
                } catch (Exception e) {
                }
            }
            if (getNextLocation() != null)
                setLocation(getNextLocation());
            else if (getCenterOnScreen())
                GUI.center(this);
            setNextLocation(null);
            setCenterOnScreen(false);
            show();
        }
    }

    private void setTextGap(int gap) {
        LswUtil.setPrivateField(this, ImageWindow.class, "textGap", gap);
    }

    private boolean getCenterOnScreen() {
        return (Boolean) LswUtil.getPrivateField(this, ImageWindow.class, "centerOnScreen");
    }

    private void setCenterOnScreen(boolean centerOnScreen) {
        LswUtil.setPrivateField(this, ImageWindow.class, "centerOnScreen", centerOnScreen);
    }

    private void setNewCanvas(boolean newCanvas) {
        LswUtil.setPrivateField(this, ImageWindow.class, "newCanvas", newCanvas);
    }

    private boolean getNewCanvas() {
        return (Boolean) LswUtil.getPrivateField(this, ImageWindow.class, "newCanvas");
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

    private ImageWindow getPreviousWindow() {
        return (ImageWindow) LswUtil.getPrivateField(this, ImageWindow.class, "previousWindow");
    }

    private Point getNextLocation() {
        return (Point) LswUtil.getPrivateField(this, ImageWindow.class, "nextLocation");
    }

}
