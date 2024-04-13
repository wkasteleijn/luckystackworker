package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Scaler;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
public class Probeersels {
    public static void main(String[] args) throws InterruptedException, IOException {

        ImagePlus image = LswFileUtil
                .openImage("C:\\Users\\wkast\\archive\\Jup\\testsession\\denoise_test.tif", OpenImageModeEnum.RGB,1,img -> img);

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

        LswFileUtil.saveImage(newImage, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_scaled.tif", true, false, false, false);

        image.updateAndDraw();
        log.info("Ians noise reduction done");

        LswUtil.waitMilliseconds(10000);

        System.exit(0);
    }

}
