package nl.wilcokas.luckystackworker.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import ij.ImagePlus;
import ij.io.Opener;
import lombok.extern.slf4j.Slf4j;
import nl.wilcokas.luckystackworker.LuckyStackWorkerContext;
import nl.wilcokas.luckystackworker.constants.Constants;
import nl.wilcokas.luckystackworker.filter.LSWSharpenFilter;
import nl.wilcokas.luckystackworker.filter.RGBBalanceFilter;
import nl.wilcokas.luckystackworker.filter.SaturationFilter;
import nl.wilcokas.luckystackworker.filter.SavitzkyGolayFilter;
import nl.wilcokas.luckystackworker.filter.SigmaFilterPlus;
import nl.wilcokas.luckystackworker.util.Util;

@Slf4j
public class WorkerService {

    private Map<String, String> properties;
    private final OperationService operationService;

    public WorkerService(Map<String, String> properties) throws IOException {
        this.properties = properties;
        final LSWSharpenFilter lswSharpenFilter = new LSWSharpenFilter();
        final RGBBalanceFilter rgbBalanceFilter = new RGBBalanceFilter();
        final SaturationFilter saturationFilter = new SaturationFilter();
        final SavitzkyGolayFilter savitzkyGolayFilter = new SavitzkyGolayFilter();
        final SigmaFilterPlus sigmaFilterPlusFilter = new SigmaFilterPlus();
        operationService = new OperationService(lswSharpenFilter, rgbBalanceFilter, saturationFilter,
                savitzkyGolayFilter, sigmaFilterPlusFilter);
    }

    public boolean processFile(final File file, boolean realtime) {
        String filePath = file.getAbsolutePath();
        Optional<String> profileOpt = Optional.ofNullable(Util.deriveProfileFromImageName(file.getAbsolutePath()));
        if (!profileOpt.isPresent()) {
            log.info("Could not determine a profile for file {}", filePath);
            return false;
        }
        String profile = profileOpt.get();
        if (realtime || profile.equals(LuckyStackWorkerContext.getActiveProfile())) {
            try {
                final String filename = Util.getImageName(Util.getIJFileFormat(filePath));
                log.info("Applying profile '{}' to: {}", profile, filename);
                if (!realtime) {
                    LuckyStackWorkerContext.statusUpdate("Processing : " + filename);
                }

                ImagePlus imp = new Opener().openImage(filePath);
                if (Util.validateImageFormat(imp, null, null)) {
                    if (Util.isPngRgbStack(imp, filePath)) {
                        imp = Util.fixNonTiffOpeningSettings(imp);
                    }
                    operationService.correctExposure(imp);
                    operationService.applyAllOperations(imp, properties, profile);
                    imp.updateAndDraw();
                    if (LuckyStackWorkerContext.getSelectedRoi() != null) {
                        imp.setRoi(LuckyStackWorkerContext.getSelectedRoi());
                        imp = imp.crop();
                    }
                    Util.saveImage(imp, getOutputFile(file), Util.isPngRgbStack(imp, filePath),
                            LuckyStackWorkerContext.getSelectedRoi() != null, false);
                    return true;
                }
            } catch (Exception e) {
                log.error("Error processing file: ", e);
            }
        }
        return false;
    }

    private String getOutputFile(final File file) {
        return Util.getPathWithoutExtension(file.getAbsolutePath()) + Constants.OUTPUT_POSTFIX + "."
                + Constants.SUPPORTED_OUTPUT_FORMAT;
    }
}
