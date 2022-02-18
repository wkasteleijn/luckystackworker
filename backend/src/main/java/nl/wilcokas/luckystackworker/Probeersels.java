package nl.wilcokas.luckystackworker;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class Probeersels {
	public static void main(String[] args) throws InterruptedException {
		ImagePlus img = IJ.openImage("C:/Jup/testsession/testsigma.png");

		// Always convert to 32-bit, saving to PNG will result anyway in the same bit
		// depth of 23 bit again in the end..
		IJ.run(img, "32-bit", "");

		img.setBorderColor(Color.BLACK);
		IJ.setForegroundColor(25, 25, 25);

		img.show();
		img.getWindow().setLocation(20, 20);
		img.setActiveChannels("111");
		img.setDisplayMode(IJ.COMPOSITE);
		img.setHideOverlay(true);

		// gamma to correct overexposure effect on 8-bit monitors
		ImageProcessor proc = img.getProcessor();
		ImageStack stack = img.getStack();
		stack.setSliceLabel(null, 1);
		//		for (int i = 1; i <= stack.getSize(); i++) {
		//			stack.getProcessor(i).gamma(0.96);
		//		}
		IJ.run(img, "Gamma...", "value=0.97");

		IJ.run(img, "AutoGamma...", "isXauto=true isYauto=true");

		//		ContrastEnhancer enh = new ContrastEnhancer();
		//		enh.equalize(img);
		//		enh.setNormalize(true);
		//		enh.setUseStackHistogram(true);
		//		double factor = 250;
		//		enh.setProcessStack(true);
		//		enh.stretchHistogram(stack.getProcessor(1), factor);
		//		enh.stretchHistogram(stack.getProcessor(2), factor);
		//		enh.stretchHistogram(stack.getProcessor(3), factor);

		Thread.currentThread().sleep(2000);

		img.getWindow().setLocation(500, 500);

		// Resizing
		//		double newWidth = img.getWidth() * 0.75;
		//		double newHeight = img.getHeight() * 0.75;
		//		IJ.run("Size...", String.format("width=%s height=%s interpolation=Bicubic", newWidth, newHeight));

		IJ.run("Out [-]");
		Thread.currentThread().sleep(2000);
		IJ.run("In [+]");

		Thread.currentThread().sleep(2000);

		IJ.run("Enhance Contrast...", "saturated=0.01");
		IJ.run("Next Slice [>]");
		IJ.run("Enhance Contrast...", "saturated=0.01");
		IJ.run("Next Slice [>]");
		IJ.run("Enhance Contrast...", "saturated=0.01");

		Thread.currentThread().sleep(2000);

		// Correct RGB using gamma
		//		stack.getProcessor(1).gamma(1);
		//		stack.getProcessor(2).gamma(0.95);
		//		stack.getProcessor(3).gamma(1);

		IJ.run(img, "Unsharp Mask...", "radius=2.0 mask=0.96");
		IJ.run(img, "ROF Denoise...", "theta=5000");


		Thread.currentThread().sleep(2000);
		System.exit(0);
	}
}
