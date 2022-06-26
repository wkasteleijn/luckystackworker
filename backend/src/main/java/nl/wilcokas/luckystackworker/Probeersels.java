package nl.wilcokas.luckystackworker;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import ij.ImagePlus;
import ij.WindowManager;
import ij.io.Opener;
import ij.macro.Interpreter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.Operations;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		ImagePlus image = new Opener()
				.openImage("D:/Jup/testsession/Jup_224759_AS_P30_lapl5_ap44_LSW.tif");
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


		// Pixel manipulation
		//		ImageProcessor p = image.getStack().getProcessor(1);
		//		short[] pixels = (short[]) p.getPixels();
		//		pixels[10 * image.getWidth() + 10] = 0;
		//		p = image.getStack().getProcessor(2);
		//		pixels = (short[]) p.getPixels();
		//		pixels[10 * image.getWidth() + 10] = 32767;
		//		p = image.getStack().getProcessor(3);
		//		pixels = (short[]) p.getPixels();
		//		pixels[10 * image.getWidth() + 10] = 32767;
		//		image.updateAndDraw();

		// Saturation
		log.info("Start saturation");
		String macro = Util.readFromInputStream(Operations.class.getResourceAsStream("/saturation.ijm"));
		StringSubstitutor stringSubstitutor = new StringSubstitutor(Map.of("factor", 3.5));
		String result = stringSubstitutor.replace(macro);
		ImagePlus image2 = image.duplicate();
		image.close();
		WindowManager.setTempCurrentImage(image2);
		new Interpreter().run(result);
		image2.show();
		log.info("End saturation");
		// Util.saveImage(image, "D:/Jup/testsession/saturation_test.tif", false);

		Thread.currentThread().sleep(5000);
		System.exit(0);
	}
}
