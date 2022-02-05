package nl.wilcokas.luckystackworker;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class Probeersels {
	public static void main(String[] args) throws InterruptedException {
		ImagePlus img = IJ.openImage("C:/Jup/250921/Jup_225635_AS_P19_lapl5_ap39.tif");

		// Always convert to 32-bit, saving to PNG will result anyway in the same bit
		// depth of 23 bit again in the end..
		IJ.run(img, "32-bit", "");

		img.show();

		// gamma to correct overexposure effect on 8-bit monitors
		ImageProcessor proc = img.getProcessor();
		ImageStack stack = img.getStack();
		//		for (int i = 1; i <= stack.getSize(); i++) {
		//			stack.getProcessor(i).gamma(0.96);
		//		}
		// IJ.run(img, "Gamma...", "value=0.95");

		// Correct RGB using gamma
		//		stack.getProcessor(1).gamma(1);
		//		stack.getProcessor(2).gamma(0.95);
		//		stack.getProcessor(3).gamma(1);

		IJ.run(img, "Unsharp Mask...", "radius=2.0 mask=0.98");
		IJ.run(img, "ROF Denoise...", "theta=5000");
		IJ.run(img, "CLAHE...",
				"blocksize=127 histogram=256 maximum=3 fast process_as_composite");

		Thread.currentThread().sleep(5000);
		System.exit(0);
	}
}
