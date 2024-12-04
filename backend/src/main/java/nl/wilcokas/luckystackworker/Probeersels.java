package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.Scaler;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.ROFDenoiseFilter;
import nl.wilcokas.luckystackworker.filter.settings.ROFParameters;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
public class Probeersels {
    public static void main(String[] args) throws InterruptedException, IOException {

        ImagePlus image = LswFileUtil
                .openImage("C:\\Users\\wkast\\archive\\Jup\\testsession\\denoise_test2.tif", OpenImageModeEnum.RGB,1,img -> img);

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();

        ROFDenoiseFilter filter = new ROFDenoiseFilter();
        filter.apply(image, ROFParameters.builder()
                .tethaRed(20000f)
                .tethaGreen(20000f)
                .tethaBlue(20000f)
                .gRed(1f)
                .gGreen(1f)
                .gBlue(1f)
                .dtRed(0.25f)
                .dtGreen(0.25f)
                .dtBlue(0.25f)
                .iterationsRed(10)
                .iterationsGreen(10)
                .iterationsBlue(10)
                .build());

        LswFileUtil.saveImage(image, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_denoised.tif", true, false, false, false);

        image.updateAndDraw();

        LswUtil.waitMilliseconds(10000);

        System.exit(0);
    }

}
