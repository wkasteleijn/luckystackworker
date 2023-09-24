package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Scaler;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class Probeersels {
    public static void main(String[] args) throws InterruptedException, IOException {

        ImagePlus image = new Opener()
                .openImage("C:\\Users\\wkast\\archive\\Jup\\testsession\\2023-09-16-0447_8-Jup_derot_AS_P13_lapl4_ap198.tif");

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();

        double factor = 2;
        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);
        ImagePlus newImage = Scaler.resize(image, newWidth, newHeight, 3, "depth=3 interpolation=Bicubic create");
        newImage.show();
        image.hide();

        Util.saveImage(newImage, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_scaled.tif", true, false, false, false);

        System.exit(0);
    }

}
