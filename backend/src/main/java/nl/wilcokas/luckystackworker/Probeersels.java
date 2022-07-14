package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		ImagePlus image = new Opener()
				.openImage("D:\\Jup\\040921\\pipp_20210905_163919\\Jup_232158_pipp_AS_P9_lapl6_ap28_LSW.tif");
		// .openImage("D:\\Jup\\testsession\\Jup_224759_AS_P30_lapl5_ap44.tif");
		// .openImage("D:\\Sun\\220522\\Sun_100434_AS_P1_lapl4_ap1531.tif");

		// IJ.run(image, "RGB Color", null);
		image.show();
		//		int[] histogram = image.getProcessor().getHistogram();
		//		int maxVal = 0;
		//		for (int i = histogram.length - 1; i >= 0; i--) {
		//			if (histogram[i] > 0) {
		//				maxVal = i;
		//				break;
		//			}
		//		}
		//		int percentage = (maxVal * 100) / 65536;
		//		System.out.println("maxVal = " + maxVal);
		//		System.out.println("percentage = " + percentage);

		// Zelf de zoom en xpos en ypos tov van origineel bijhouden: zoom :75, 100, 150,
		// 200, 300, 400, 600, ..
		// xpos: bij 200% zoom op 1024 grootte image: 1024-512/2 = 256
		// ypos : etc
		//		int zoom = 100;
		//		IJ.run("In [+]");
		//		zoom += 50;
		//		IJ.run("In [+]");
		//		zoom += 50;
		//		IJ.run("In [+]");
		//		zoom += 100;
		//		IJ.run("In [+]");
		//		zoom += 100;

		//		Thread.currentThread().sleep(2000);
		//		int xpos = image.getRoi().size();
		//		int ypos = image.getProcessor().getRoi().y;
		//		int newXPos = xpos - 16;
		//		int newYPos = ypos - 16;
		//
		//		IJ.run("Set... ", "zoom=" + zoom + " x=" + newXPos + " y=" + newYPos);
		// IJ.run(image, "Unsharp Mask...", "radius=2.0 mask=0.97");
		// IJ.run(image, "SigmaFilterPlus...", "radius=2 use=2 minimum=1 outlier");

		// File suggestie bij save dialoog:
		// https://stackoverflow.com/questions/356671/how-do-i-set-a-suggested-file-name-using-jfilechooser-showsavedialog
		// jFileChooser.setSelectedFile(new File("fileToSave.txt"));


		// Thread.currentThread().sleep(5000);

		// Pixel manipulation
		// 16-bit short values range from 0 - 32767. Values larger than 32767 will be
		// subtracted by 65536. So the highest possible value = -1, with the lowest
		// value being 0;

		//		log.info("Start manipulation");
		//		int maxHistogramVal = Util.getMaxHistogramPercentage(image);
		//		double correctionFactor = Constants.DEFAULT_EXP_CORRECTION_FACTOR;
		//		if (maxHistogramVal < 50) {
		//			correctionFactor = 1.9;
		//		} else if (maxHistogramVal > 70) {
		//			correctionFactor = 1.25;
		//		} else if (maxHistogramVal > 60) {
		//			correctionFactor = 1.5;
		//		}
		//		log.debug("maxHistogramPercentage = " + maxHistogramVal);
		//		for (int layer = 1; layer <= 3; layer++) {
		//			ImageProcessor p = image.getStack().getProcessor(layer);
		//			short[] pixels = (short[]) p.getPixels();
		//			for (int i = 0; i < pixels.length; i++) {
		//				int newValue = (int) ((pixels[i] < 0 ? 65536 + pixels[i] : pixels[i]) / correctionFactor);
		//				if (newValue >= 0) {
		//					pixels[i] = (short) (newValue > 32767 ? newValue - 65536 : newValue);
		//				} else {
		//					pixels[i] = 0;
		//				}
		//			}
		//		}
		//		image.updateAndDraw();
		//		log.info("End manipulation");

		// Set exposure back to original value
		image.setDefault16bitRange(16);
		image.resetDisplayRange();
		image.updateAndDraw();

		for (int layer = 1; layer <= 3; layer++) {
			ImageProcessor p = image.getStack().getProcessor(layer);
			// p.setHistogramRange(20000, 30000);
			p.setThreshold(10000, 40000, ImageProcessor.OVER_UNDER_LUT);
			// p.resetThreshold();
		}
		image.setSlice(0);
		image.getProcessor().setMinAndMax(10000, 40000);
		image.setSlice(1);
		image.getProcessor().setMinAndMax(10000, 40000);
		image.setSlice(2);
		image.getProcessor().setMinAndMax(10000, 40000);
		// image.resetDisplayRange();
		image.updateAndDraw();

		// for (int layer = 1; layer <= 3; layer++) {
		// ImageProcessor p = image.getStack().getProcessor(layer);
		// image.setDisplayRange(minHistogram.getLeft(), maxHistogram.getLeft(), layer);
		// }
		// image.updateAndDraw();
		// call("ij.ImagePlus.setDefault16bitRange", 16);

		// Histogram stretching
		//		Pair<Integer, Integer> minHistogram = Util.getMinHistogram(image);
		//		Pair<Integer, Integer> maxHistogram = Util.getMaxHistogram(image);
		//		for (int layer = 1; layer <= 3; layer++) {
		//			ImageProcessor p = image.getStack().getProcessor(layer);
		//			image.setDisplayRange(minHistogram.getLeft(), maxHistogram.getLeft(), layer);
		//		}
		//		image.updateAndDraw();
		//		log.info("End manipulation");

		//		int averageHistogram = (maxHistogram.getLeft() + minHistogram.getLeft()) / 2;
		//		double stretchFactor = 1.1;
		//		for (int layer = 1; layer <= 3; layer++) {
		//			ImageProcessor p = image.getStack().getProcessor(layer);
		//			short[] pixels = (short[]) p.getPixels();
		//			for (int i = 0; i < pixels.length; i++) {
		//				int actualPixelValue = pixels[i] < 0 ? 65536 + pixels[i] : pixels[i];
		//				int newValue = averageHistogram;
		//				if (actualPixelValue > averageHistogram) {
		//					newValue = (int) (actualPixelValue * stretchFactor);
		//				} else if (actualPixelValue < averageHistogram) {
		//					newValue = (int) (actualPixelValue / stretchFactor);
		//				}
		//				pixels[i] = (short) (newValue > 32767 ? newValue - 65536 : newValue);
		//			}
		//		}
		//		image.updateAndDraw();


		// Saturation
		//		log.info("Start saturation");
		//		String macro = Util.readFromInputStream(Operations.class.getResourceAsStream("/saturation.ijm"));
		//		StringSubstitutor stringSubstitutor = new StringSubstitutor(Map.of("factor", 3.5));
		//		String result = stringSubstitutor.replace(macro);
		//		WindowManager.setTempCurrentImage(image);
		//		new Interpreter().run(result);

		// cropping
		//		image.setRoi(128, 128, 640, 480);
		//		ImagePlus crop = image.crop();

		Thread.currentThread().sleep(5000);
		System.exit(0);
	}
}
