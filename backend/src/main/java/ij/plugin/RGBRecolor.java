package ij.plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/*	RGB_Recolor - ImageJ plug-in that allows linear alteration of
 *	the colors in the red, green and blue planes of
 *	an RGB image or stack, where:
 *	New color = Old Color*Scaling Factor + Constant
 *	If the new color is greater than 255 it is set to 255
 *	If the new color is less than 0 it is set to 0
 *	by Kieran Holland
 */

public class RGBRecolor implements PlugInFilter {
	ImagePlus imp;
	static double[] rgbF = new double[3];
	static double[] rgbC = new double[3];
	static boolean canceled;
	boolean sameSettings = false;
	int slice;

	static {
		for (int i = 0; i < 3; i++) {
			rgbF[i] = 1.0;
			rgbC[i] = 0.0;
		}
	} // static initializer

	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}
		this.imp = imp;
		slice = 1;
		canceled = false;
		IJ.register(RGBRecolor.class);
		return DOES_RGB + DOES_STACKS;
	}

	@Override
	public void run(ImageProcessor ip) {
		if (canceled)
			return;
		int w = imp.getWidth();
		int h = imp.getHeight();
		int size = imp.getStackSize();
		if (!sameSettings) {
			GenericDialog gd = new GenericDialog("RGB Recoloring Options");
			gd.addMessage("Linear adjustment of RGB color planes where:\n"
					+ "New Color = Old Color * Scaling Factor + Constant");
			gd.addMessage("");
			gd.addNumericField("rfactor", rgbF[0], 2);
			gd.addNumericField("rconstant", rgbC[0], 2);
			gd.addNumericField("gfactor", rgbF[1], 2);
			gd.addNumericField("gconstant", rgbC[1], 2);
			gd.addNumericField("bfactor", rgbF[2], 2);
			gd.addNumericField("bconstant", rgbC[2], 2);
			if (size > 1)
				gd.addCheckbox("Use these parameters for rest of stack (" + slice + "/" + size + ")", sameSettings);
			gd.showDialog();
			if (gd.wasCanceled()) {
				canceled = true;
				return;
			}
			if (gd.invalidNumber()) {
				IJ.error("Invalid parameter entered.");
				canceled = true;
				return;
			}
			for (int i = 0; i < 3; i++) {
				rgbF[i] = gd.getNextNumber();
				rgbC[i] = gd.getNextNumber();
			}
			if (size > 1)
				sameSettings = gd.getNextBoolean();
		}

		byte[][] rgb;
		ColorProcessor cp = (ColorProcessor) ip;
		rgb = new byte[3][w * h];
		cp.getRGB(rgb[0], rgb[1], rgb[2]);
		for (int i = 0; i < w * h; i++) {
			double x;
			for (int j = 0; j < 3; j++) {
				x = (0xff & rgb[j][i]) * rgbF[j] + rgbC[j];
				if (x > 255)
					x = 255;
				if (x < 0)
					x = 0;
				rgb[j][i] = (byte) x;
			}
		}
		cp.setRGB(rgb[0], rgb[1], rgb[2]);
		if (!sameSettings)
			imp.setSlice(slice);
		slice++;
	}

	void showAbout() {
		IJ.showMessage("About RGB_Recolor...",
				"This plug-in filter allows you to linearly adjust the RGB planes in an RGB image or stack\n");
	}
}
