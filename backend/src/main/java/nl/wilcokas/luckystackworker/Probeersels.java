package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		// ImageJ ij = new net.imagej.ImageJ();
		// Object imagePng =
		// ij.io().open("C:\\Jup\\040921\\pipp_20210905_163919\\Jup_232158_pipp_AS_P13_lapl7_ap20.png");
		// ij.ui().show(imagePng);
		ImagePlus image = new Opener().openImage("C:/Jup/testsession/Jup_224759_AS_P30_lapl5_ap44.tif");

		IJ.run(image, "Unsharp Mask...", "radius=2.0 mask=0.97");
		IJ.run(image, "SigmaFilterPlus...", "radius=2 use=2 minimum=1 outlier");

		//		Img converted = ImageJFunctions.wrap(img);
		//		new ImgSaver().saveImg("C:\\Jup\\040921\\pipp_20210905_163919\\image.tif", converted);
		// ij.context().service(LegacyService.class).runLegacyCommand("Unsharp Mask",
		// "radius=2.0 mask=0.97");
		image.setActiveChannels("111");
		image.setC(1);
		image.setZ(1);
		FileSaver saver = new FileSaver(image);
		saver.saveAsTiffStack("C:/Jup/testsession/Jup_224759_AS_P30_lapl5_ap44_2.tif");
		System.exit(0);
	}
}
