package nl.wilcokas.luckystackworker;

import java.io.IOException;

import edu.emory.mathcs.restoretools.Enums;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver2D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.filter.ROFDenoiseFilter;
import nl.wilcokas.luckystackworker.filter.WienerDeconvolutionFilter;
import nl.wilcokas.luckystackworker.model.Profile;
import nl.wilcokas.luckystackworker.service.dto.OpenImageModeEnum;
import nl.wilcokas.luckystackworker.util.LswFileUtil;
import nl.wilcokas.luckystackworker.util.LswUtil;

@Slf4j
public class Probeersels {
    public static void main(String[] args) throws InterruptedException, IOException {

//        new ImageJ();
//        IJ.open("D:\\Jup\\311024\\cropped2\\jup_lsw\\wiener.png");
//        IJ.open("D:\\Jup\\311024\\cropped2\\jup_lsw\\psf_2.png");
//        IJ.runPlugIn("edu.emory.mathcs.restoretools.iterative.ParallelIterativeDeconvolution2D", null);

        // /*
        ImagePlus image = LswFileUtil
                .openImage("D:\\Jup\\311024\\cropped2\\jup_lsw\\2024-10-31-2300_2-U-RGB-Jup_pipp_europa_AS_P25_lapl6_ap303_LSW.png", OpenImageModeEnum.RGB, 1, img -> img);
        ImagePlus psf = new Opener().openImage(LswFileUtil.getIJFileFormat("D:\\Jup\\311024\\cropped2\\jup_lsw\\psf_2.png"));

        image.show();
        // Set exposure back to original value
        image.setDefault16bitRange(16);
        image.resetDisplayRange();
        image.updateAndDraw();

        log.info("Start Wiener deconvolution");
        WienerDeconvolutionFilter filter = new WienerDeconvolutionFilter();
        filter.apply(image,psf,15);
        log.info("Completed Wiener deconvolution");

        LswFileUtil.saveImage(image, "jup", "C:/Users/wkast/archive/Jup/testsession/jup_denoised2.tif", true, false, false, false);

        image.updateAndDraw();

        LswUtil.waitMilliseconds(10000);

        System.exit(0);
        // */
    }

}
