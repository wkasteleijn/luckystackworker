package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.IansNoiseReductionFilter;
import nl.wilcokas.luckystackworker.filter.settings.IansNoiseReductionParameters;

@Slf4j
public class Probeersels {
    public static void main(String[] args) throws InterruptedException, IOException {

        ImagePlus image = new Opener()
                .openImage("C:\\Users\\wkast\\archive\\Jup\\testsession\\denoise_test.tif");

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();

        //        double factor = 2;
        //        int newWidth = (int) (image.getWidth() * factor);
        //        int newHeight = (int) (image.getHeight() * factor);
        //        ImagePlus newImage = Scaler.resize(image, newWidth, newHeight, 3, "depth=3 interpolation=Bicubic create");
        //        newImage.show();
        //        image.hide();
        //
        //        Util.saveImage(newImage, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_scaled.tif", true, false, false, false);

        IansNoiseReductionParameters parameters = IansNoiseReductionParameters.builder().fine(5).recover(true).build();
        IansNoiseReductionFilter iansNoiseReductionFilter = new IansNoiseReductionFilter();
        log.info("Start Ians noise reduction");
        iansNoiseReductionFilter.apply(image, "jup", parameters, false);
        image.updateAndDraw();
        log.info("Ians noise reduction done");

        Thread.currentThread().sleep(10000);

        System.exit(0);
    }

}
