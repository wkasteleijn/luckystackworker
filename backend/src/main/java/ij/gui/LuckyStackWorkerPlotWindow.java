package ij.gui;

import java.awt.LayoutManager;
import java.awt.Panel;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class LuckyStackWorkerPlotWindow extends PlotWindow {

    private Plot plot;

    public LuckyStackWorkerPlotWindow(ImagePlus imp, Plot plot) {
        super(imp, plot);
        this.plot = plot;
    }

    @Override
    public void draw() {
        Panel bottomPanel = new Panel();
        int hgap = IJ.isMacOSX() ? 1 : 5;

        LayoutManager lm = getLayout();
        if (lm instanceof ImageLayout)
            ((ImageLayout) lm).ignoreNonImageWidths(true); // don't expand size to make the panel fit
        GUI.scale(bottomPanel);
        maximizeCoordinatesLabelWidth();
        pack();

        ImageProcessor ip = plot.getProcessor();
        boolean ipIsColor = ip instanceof ColorProcessor;
        boolean impIsColor = imp.getProcessor() instanceof ColorProcessor;
        if (ipIsColor != impIsColor)
            imp.setProcessor(null, ip);
        else
            imp.updateAndDraw();
        if (listValues)
            showList(/*useLabels=*/false);
        else
            ic.requestFocus(); // have focus on the canvas, not the button, so that pressing the space bar
        // allows panning
    }
}
