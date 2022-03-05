package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import ij.process.LUT;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		ImagePlus image = new Opener()
				.openImage("C:/Jup/testsession/Jup_224759_AS_P30_lapl5_ap44.tif");
		image.show();

		ImageProcessor processor = image.getProcessor();
		LUT lut = new LUT(processor.getLut().getColorModel(), -3000, 60984);
		image.setLut(lut);
		image.updateAndDraw();

		image.setSlice(2);
		processor = image.getProcessor();
		lut = new LUT(processor.getLut().getColorModel(), -3000, 60984);
		image.setLut(lut);
		image.updateAndDraw();

		image.setSlice(3);
		processor = image.getProcessor();
		lut = new LUT(processor.getLut().getColorModel(), -3000, 60984);
		image.setLut(lut);
		image.updateAndDraw();

		IJ.run(image, "Unsharp Mask...", "radius=2.0 mask=0.98");
		IJ.run(image, "SigmaFilterPlus...", "radius=2 use=2 minimum=1 outlier");

		Thread.currentThread().sleep(4000);
		System.exit(0);
	}
}
