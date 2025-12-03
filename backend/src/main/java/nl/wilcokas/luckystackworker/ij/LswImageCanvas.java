package nl.wilcokas.luckystackworker.ij;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import java.awt.*;

public class LswImageCanvas extends ImageCanvas {

    public LswImageCanvas(ImagePlus imp) {
        super(imp);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public double getMagnification() {
        return super.magnification;
    }
}
