package nl.wilcokas.luckystackworker;

import java.io.IOException;

import ij.ImagePlus;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.ROFDenoiseFilter;
import nl.wilcokas.luckystackworker.model.Profile;
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
        filter.apply(image, Profile.builder()
                .rofTheta(20000)
                .rofThetaGreen(20000)
                .rofThetaBlue(20000)
                .rofIterations(15)
                .rofIterationsGreen(15)
                .rofIterationsBlue(15)
                .build());

        LswFileUtil.saveImage(image, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_denoised2.tif", true, false, false, false);

        image.updateAndDraw();

        LswUtil.waitMilliseconds(10000);

        System.exit(0);
    }

}
