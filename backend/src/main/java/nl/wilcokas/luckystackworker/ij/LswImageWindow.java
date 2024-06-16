package nl.wilcokas.luckystackworker.ij;

import ij.*;
import ij.gui.*;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;
import org.springframework.util.ReflectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
public class LswImageWindow extends ImageWindow implements MouseMotionListener {

    private final double SCALE = Prefs.getGuiScale();
    private int TEXT_GAP = 11;

    private ImagePlus image;

    private Point dragPreviousLocation;

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
        ic = new ImageCanvas(imp);
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
            }
        }
        show();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int thickness = 1;
        int width = getWidth();
        int height = getHeight();
        g.setColor(new Color(32, 32, 32));
        g.fillRect(0, 0, width, 32);
        g.setColor(new Color(64, 64, 64));
        g.fillRect(0, 0, width, thickness);
        g.fillRect(0, height-1, width, thickness);
        g.fillRect(0, 0, thickness, height);
        g.fillRect(width - thickness, 0, thickness, height);
        g.setColor(new Color(100, 100, 100));
        int textWidth = g.getFontMetrics().stringWidth(image.getTitle());
        int x = (getWidth() - textWidth) / 2;
        g.drawString(image.getTitle(), x, 22);
    }

    public void zoomIn() {
        int x = ic.screenX(image.getWidth() / 2);
        int y = ic.screenY(image.getHeight() / 2);
        ic.zoomIn(x, y);
    }

    public void zoomOut() {
        int x = ic.screenX(image.getWidth() / 2);
        int y = ic.screenY(image.getHeight() / 2);
        ic.zoomOut(x, y);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        synchronized (this) {
            this.setLocation(e.getXOnScreen() - (image.getWidth()/2), e.getYOnScreen() - 16);
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e.getX(), e.getY());
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
}
