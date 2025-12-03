package nl.wilcokas.luckystackworker.util;

import ij.ImagePlus;
import ij.io.FileSaver;

public class LSWFileSaver extends FileSaver {

    public LSWFileSaver(ImagePlus imp) {
        super(imp);
    }

    @Override
    public String getDescriptionString() {
        return "Created with LuckyStackWorker";
    }
}
