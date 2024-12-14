package nl.wilcokas.luckystackworker;

import java.io.IOException;

import edu.emory.mathcs.restoretools.Enums;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.IJ;
import ij.ImageJ;
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

        new ImageJ();
        IJ.open("D:\\Jup\\311024\\cropped2\\jup_lsw\\wiener.png");
        IJ.open("D:\\Jup\\311024\\cropped2\\jup_lsw\\psf_2.png");
        IJ.runPlugIn("edu.emory.mathcs.restoretools.iterative.ParallelIterativeDeconvolution2D", null);

        /*
        ImagePlus image = LswFileUtil
                .openImage("D:\\Jup\\\\311024\\cropped2\\jup_lsw\\wiener.png", OpenImageModeEnum.RGB, 1, img -> img);
        ImagePlus psf = LswFileUtil
                .openImage("D:\\Jup\\\\311024\\cropped2\\jup_lsw\\psf_2.png", OpenImageModeEnum.RGB, 1, img -> img);

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();

        WPLFloatIterativeDeconvolver2D deconv = new WPLFloatIterativeDeconvolver2D(image, psf, IterativeEnums.BoundaryType.REFLEXIVE, IterativeEnums.ResizingType.AUTO,
                Enums.OutputType.SAME_AS_SOURCE, 5, false, new WPLOptions());
        ImagePlus deconvImage = deconv.deconvolve();
        deconvImage.show();

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
        */
    }

}
