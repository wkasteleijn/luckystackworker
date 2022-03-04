package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Probeersels {
	public static void main(String[] args) throws InterruptedException, IOException {

		ImagePlus image = new Opener()
				.openImage("C:/Jup/040921/pipp_20210905_163919/Jup_232158_pipp_AS_P9_lapl6_ap28.tif");
		int[] histogram = image.getProcessor().getHistogram();
		int maxVal = 0;
		for (int i = histogram.length - 1; i >= 0; i--) {
			if (histogram[i] > 0) {
				maxVal = i;
				break;
			}
		}
		int percentage = (maxVal * 100) / 65536;
		System.out.println("maxVal = " + maxVal);
		System.out.println("percentage = " + percentage);

		// IJ.run(image, "Unsharp Mask...", "radius=2.0 mask=0.97");
		// IJ.run(image, "SigmaFilterPlus...", "radius=2 use=2 minimum=1 outlier");

		// Thread.currentThread().sleep(2000);
		System.exit(0);
	}
}
