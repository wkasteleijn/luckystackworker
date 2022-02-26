package nl.wilcokas.luckystackworker;

import java.awt.Color;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.Opener;
import ij.macro.Interpreter;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.Operations;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		// ImagePlus img =
		// IJ.openImage("C:/Sun/230222/Sun_130326_AS_P3_lapl3_ap977.tif");
		ImagePlus img = new Opener().openImage("C:/Jup/250921/Jup_224936_AS_P26_lapl5_ap44.tif");
		img.show();

		String macro = Util.readFromInputStream(Operations.class.getResourceAsStream("/correct_exposure.ijm"));
		boolean isStack = img.getStack() !=null && img.getStack().size()>1;
		log.info("img.getStack().size() == " + img.getStack().size());
		StringSubstitutor stringSubstitutor = new StringSubstitutor(
				Map.of("level", 3, "isStack", isStack));
		String result = stringSubstitutor.replace(macro);
		WindowManager.setTempCurrentImage(img);
		Interpreter interpreter = new Interpreter();
		interpreter.run(result);

		img.setColor(Color.BLACK);
		// ij.setForegroundColor(25, 25, 25);

		//		img.show();
		//		img.getWindow().setLocation(20, 20);
		//		img.setActiveChannels("111");
		//		img.setDisplayMode(IJ.COMPOSITE);
		//		img.setHideOverlay(true);

		// gamma to correct overexposure effect on 8-bit monitors

		ImageStack stack = img.getStack();
		stack.setSliceLabel(null, 1);
		//		for (int i = 1; i <= stack.getSize(); i++) {
		//			stack.getProcessor(i).gamma(0.96);
		//		}

		//		ContrastEnhancer enh = new ContrastEnhancer();
		//		enh.equalize(img);
		//		enh.setNormalize(true);
		//		enh.setUseStackHistogram(true);
		//		double factor = 250;
		//		enh.setProcessStack(true);
		//		enh.stretchHistogram(stack.getProcessor(1), factor);
		//		enh.stretchHistogram(stack.getProcessor(2), factor);
		//		enh.stretchHistogram(stack.getProcessor(3), factor);

		// Resizing
		//		double newWidth = img.getWidth() * 0.75;
		//		double newHeight = img.getHeight() * 0.75;
		//		IJ.run("Size...", String.format("width=%s height=%s interpolation=Bicubic", newWidth, newHeight));

		// IJ.run("Out [-]");
		// Thread.currentThread().sleep(2000);
		// IJ.run("In [+]");

		//		IJ.run("Enhance Contrast...", "saturated=0.01");
		//		IJ.run("Next Slice [>]");
		//		IJ.run("Enhance Contrast...", "saturated=0.01");
		//		IJ.run("Next Slice [>]");
		//		IJ.run("Enhance Contrast...", "saturated=0.01");

		// Correct RGB using gamma
		//		stack.getProcessor(1).gamma(1);
		//		stack.getProcessor(2).gamma(0.95);
		//		stack.getProcessor(3).gamma(1);

		IJ.run(img, "Unsharp Mask...", "radius=2.0 mask=0.97");
		Thread.currentThread().sleep(2000);

		// IJ.run(img, "ROF Denoise...", "theta=5000");
		IJ.run(img, "SigmaFilterPlus...", "radius=2 use=2 minimum=1 outlier");

		Thread.currentThread().sleep(2000);
		System.exit(0);
	}
}
