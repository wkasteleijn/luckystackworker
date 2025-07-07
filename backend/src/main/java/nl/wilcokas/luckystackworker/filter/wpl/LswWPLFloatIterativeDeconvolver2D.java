package nl.wilcokas.luckystackworker.filter.wpl;

import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.jet.math.tfloat.FloatFunctions;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.iterative.FloatCommon2D;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.PaddingType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.ResizingType;
import edu.emory.mathcs.utils.pc.ConcurrencyUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Wiener Filter Preconditioned Landweber 2D. This is a nonnegatively constrained method.
 *
 * @author Bob Dougherty
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * Adapted by Wilco Kasteleijn for use in LuckyStackWorker.
 */
@Slf4j
public class LswWPLFloatIterativeDeconvolver2D {

    /**
     * Blurred image
     */
    protected FloatMatrix2D B;
    /**
     * Point Spread Function
     */
    protected FloatMatrix2D PSF;
    /**
     * Color model
     */
    protected java.awt.image.ColorModel cmY;

    /**
     * Number of columns in the blurred image.
     */
    protected int bColumns;

    /**
     * Number of rows in the blurred image.
     */
    protected int bRows;

    /**
     * Maximal number of iterations.
     */
    protected int iterations;

    /**
     * If true, then the thresholding is performed.
     */
    protected boolean useThreshold;

    /**
     * The smallest nonnegative value assigned to the restored image.
     */
    protected float threshold;

    /**
     * If true, then the convergence information is displayed after each
     * iteration.
     */
    protected boolean logConvergence;

    /**
     * Number of columns in the expanded blurred image.
     */
    private int columns;

    /**
     * Number of rows in the expanded blurred image.
     */
    private int rows;

    /**
     * Min value of the blurred image.
     */
    private float minB = 0;

    /**
     * Min value of the PSF.
     */
    private float minPSF = 0;

    /**
     * Sum of all elements in the PSF matrix.
     */
    private float sum;

    /**
     * Scaling factor.
     */
    private float scalePSF = 1;

    /**
     * Gaussian weights.
     */
    private float[][] gweights;

    /**
     * Regularization parameter for the Wiener Filter.
     */
    protected float gamma;

    /**
     * Number of pixels in x and y directions for low-pass filter.
     */
    protected float filterXY;

    /**
     * Number of pixels in z direction for low-pass filter.
     */
    protected float filterZ;
    /**
     * If true, then PSF is normalized.
     */
    protected boolean normalize;

    /**
     * If true, then the anti-ringing step is performed.
     */
    protected boolean antiRing;

    /**
     * This parameter is used to stop the iteration if the image is not
     * changing.
     */
    protected float changeThreshPercent;

    /**
     * If true, then all the data is in decibels.
     */
    protected boolean dB;

    /**
     * If true then the iterations are stopped when the changes appear to be
     * increasing.
     */
    protected boolean detectDivergence;

    /**
     * Number of virtual threads created for parallel processing.
     */
    private int numberOfThreads;

    /**
     * Creates a new instance of WPLFloatIterativeDeconvolver2D
     *
     * @param imB        blurred image
     * @param imPSF      Point Spread Function
     * @param boundary   type of boundary conditions
     * @param resizing   type of resizing
     * @param iterations maximal number of iterations
     * @param options    WPL options
     */
    public LswWPLFloatIterativeDeconvolver2D(ImagePlus imB, ImagePlus imPSF, BoundaryType boundary, ResizingType resizing, int iterations, WPLOptions options, int numberOfThreads) {
        log.info("WPL initialization...");
        ImageProcessor ipB = imB.getProcessor();
        cmY = ipB.getColorModel();
        bColumns = ipB.getWidth();
        bRows = ipB.getHeight();
        B = FloatCommon2D.assignPixelsToMatrix(ipB);

        ImageProcessor ipPSF = imPSF.getProcessor();
        int psfColumns = ipPSF.getWidth();
        int psfRows = ipPSF.getHeight();
        PSF = FloatCommon2D.assignPixelsToMatrix(ipPSF);
        this.iterations = iterations;
        this.gamma = (float) options.getGamma();
        this.filterXY = (float) options.getFilterXY();
        this.filterZ = (float) options.getFilterZ();
        this.normalize = options.isNormalize();
        this.antiRing = options.isAntiRing();
        this.changeThreshPercent = (float) options.getChangeThreshPercent();
        this.dB = options.isDB();
        this.detectDivergence = options.isDetectDivergence();
        this.logConvergence = options.isLogConvergence();
        if (this.dB) {
            minB = unDB(B);
            minPSF = unDB(PSF);
        }
        sum = PSF.zSum();
        if ((sum != 0) && this.normalize)
            scalePSF /= sum;

        columns = expandedSize(psfColumns, bColumns, resizing);
        rows = expandedSize(psfRows, bRows, resizing);
        if ((psfColumns > columns) || (psfRows > rows)) {
            throw new IllegalArgumentException("PSF cannot be largest that the image.");
        }
        gweights = gaussianWeights(rows, columns, this.filterXY, this.filterXY);
        switch (boundary) {
            case PERIODIC:
                B = FloatCommon2D.padPeriodic(B, rows, columns);
                break;
            case REFLEXIVE:
                B = FloatCommon2D.padReflexive(B, rows, columns);
                break;
            case ZERO:
                B = FloatCommon2D.padZero(B, rows, columns);
                break;
        }
        float[] maxLoc = PSF.getMaxLocation();
        int[] padSize = new int[2];
        padSize[0] = rows - psfRows;
        padSize[1] = columns - psfColumns;
        PSF = FloatCommon2D.padZero(PSF, padSize, PaddingType.POST);
        PSF = FloatCommon2D.circShift(PSF, new int[]{(int) maxLoc[1], (int) maxLoc[2]});
        this.numberOfThreads = numberOfThreads;
    }

    /**
     * Performs deconvolution and returns deconvolved image.
     *
     * @return deconvolved image
     */
    public ImagePlus deconvolve() throws ExecutionException, InterruptedException {
        ((DenseFloatMatrix2D) PSF).dht2();
        FloatMatrix2D X;
        FloatMatrix2D AX = B.like();
        if (antiRing) {
            log.info("WPL: performing anti-ringing step.");
            X = B.copy();
            ((DenseFloatMatrix2D) X).dht2();
            convolveFD(rows, columns, PSF, X, AX);
            ((DenseFloatMatrix2D) AX).idht2(true);
            copyDataAverage(bRows, bColumns, rows, columns, sum, B, AX, B);
        }
        if (gamma > 0.0001) {
            log.info("WPL: Wiener filter");
            float magMax = findMagMax(PSF);
            ((DenseFloatMatrix2D) B).dht2();
            X = PSF.copy();
            deconvolveFD(gamma, magMax, rows, columns, X, X, PSF);
            AX = B.copy();
            deconvolveFD(gamma, magMax, rows, columns, AX, X, B);
            ((DenseFloatMatrix2D) B).idht2(true);
        }

        int rOff = (rows - bRows + 1) / 2;
        int cOff = (columns - bColumns + 1) / 2;

        ((DenseFloatMatrix2D) PSF).idht2(true);
        float aSum = PSF.aggregate(FloatFunctions.plus, FloatFunctions.abs);
        if (scalePSF != 1) {
            B.assign(FloatFunctions.div(scalePSF));
        }
        ((DenseFloatMatrix2D) PSF).dht2();
        X = B.copy();
        ImagePlus imX = null;
        FloatProcessor ip = new FloatProcessor(bColumns, bRows);
        for (int iter = 0; iter < iterations; iter++) {
            log.info("WPL iteration: " + (iter + 1) + "/" + iterations);
            ((DenseFloatMatrix2D) X).dht2();
            gaussianFilter(X, gweights);
            convolveFD(rows, columns, PSF, X, AX);
            ((DenseFloatMatrix2D) AX).idht2(true);
            ((DenseFloatMatrix2D) X).idht2(true);
            meanDelta(B, AX, X, aSum);
            energySum(X, bRows, bColumns, cOff, rOff);
        }
        ((DenseFloatMatrix2D) X).dht2();
        gaussianFilterWithScaling(X, gweights, aSum);
        ((DenseFloatMatrix2D) X).idht2(true);
        if (dB) {
            toDB(PSF, minPSF);
            toDB(B, minB);
            toDB(X, -90);
        }
        if (threshold == -1.0) {
            FloatCommon2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY);
        } else {
            FloatCommon2D.assignPixelsToProcessorPadded(ip, X, bRows, bColumns, rOff, cOff, cmY, threshold);
        }
        imX = new ImagePlus("(deblurred)", ip);
        FloatCommon2D.convertImage(imX, OutputType.SHORT);
        return imX;
    }

    private void convolveFD(final int rows, final int columns, FloatMatrix2D H1, FloatMatrix2D H2, FloatMatrix2D Result) throws ExecutionException, InterruptedException {
        final float[] h1 = (float[]) H1.elements();
        final float[] h2 = (float[]) H2.elements();
        final float[] result = (float[]) Result.elements();
        int np = numberOfThreads;
        if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int k = rows / np;
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        int cC, rC, idx1, idx2;
                        float h2e, h2o;
                        for (int r = firstRow; r < lastRow; r++) {
                            rC = (rows - r) % rows;
                            for (int c = 0; c < columns; c++) {
                                cC = (columns - c) % columns;
                                idx1 = c + columns * r;
                                idx2 = cC + columns * rC;
                                h2e = (h2[idx1] + h2[idx2]) / 2;
                                h2o = (h2[idx1] - h2[idx2]) / 2;
                                result[idx1] = (h1[idx1] * h2e + h1[idx2] * h2o);
                            }

                        }
                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }
        } else {
            int cC, rC, idx1, idx2;
            float h2e, h2o;
            for (int r = 0; r < rows; r++) {
                rC = (rows - r) % rows;
                for (int c = 0; c < columns; c++) {
                    cC = (columns - c) % columns;
                    idx1 = c + columns * r;
                    idx2 = cC + columns * rC;
                    h2e = (h2[idx1] + h2[idx2]) / 2;
                    h2o = (h2[idx1] - h2[idx2]) / 2;
                    result[idx1] = (h1[idx1] * h2e + h1[idx2] * h2o);
                }
            }
        }
    }

    private void copyDataAverage(final int rows, final int columns, final int rowsE, final int columnsE, final float sum, FloatMatrix2D DataIn, FloatMatrix2D DataOut, FloatMatrix2D Result) throws ExecutionException, InterruptedException {
        final float[] dataIn = (float[]) DataIn.elements();
        final float[] dataOut = (float[]) DataOut.elements();
        final float[] result = (float[]) Result.elements();

        final int rOff = (rowsE - rows + 1) / 2;
        final int cOff = (columnsE - columns + 1) / 2;
        int np = numberOfThreads;
        if ((np > 1) && (columnsE * rowsE >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                int k = rowsE / np;
                for (int j = 0; j < np; j++) {
                    final int firstRow = -rOff + j * k;
                    final int lastRow = (j == np - 1) ? rowsE - rOff : firstRow + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        int cOut, rOut, idx;
                        float alphaI, alphaJ;
                        float a;
                        for (int r = firstRow; r < lastRow; r++) {
                            rOut = r + rOff;
                            if (r < 0) {
                                alphaJ = -r / ((float) rOff);
                            } else if (r > (rows - 1)) {
                                alphaJ = (r - rows) / ((float) rOff);
                            } else {
                                alphaJ = 0;
                            }
                            for (int c = -cOff; c < columnsE - cOff; c++) {
                                cOut = c + cOff;
                                if (c < 0) {
                                    alphaI = -c / ((float) cOff);
                                } else if (c > (columns - 1)) {
                                    alphaI = (c - columns) / ((float) cOff);
                                } else {
                                    alphaI = 0;
                                }
                                a = alphaJ;
                                if (alphaI > a)
                                    a = alphaI;
                                idx = cOut + columnsE * rOut;
                                result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
                            }
                        }
                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }

        } else {
            int cOut, rOut, idx;
            float alphaI, alphaJ;
            float a;
            for (int r = -rOff; r < rowsE - rOff; r++) {
                rOut = r + rOff;
                if (r < 0) {
                    alphaJ = -r / ((float) rOff);
                } else if (r > (rows - 1)) {
                    alphaJ = (r - rows) / ((float) rOff);
                } else {
                    alphaJ = 0;
                }
                for (int c = -cOff; c < columnsE - cOff; c++) {
                    cOut = c + cOff;
                    if (c < 0) {
                        alphaI = -c / ((float) cOff);
                    } else if (c > (columns - 1)) {
                        alphaI = (c - columns) / ((float) cOff);
                    } else {
                        alphaI = 0;
                    }
                    a = alphaJ;
                    if (alphaI > a)
                        a = alphaI;
                    idx = cOut + columnsE * rOut;
                    result[idx] = (1 - a) * dataIn[idx] + a * dataOut[idx] / sum;
                }
            }
        }
    }

    private void deconvolveFD(final float gamma, final float magMax, final int rows, final int columns, FloatMatrix2D H1, FloatMatrix2D H2, FloatMatrix2D Result) throws ExecutionException, InterruptedException {
        final float gammaScaled = gamma * magMax;
        final float[] h1 = (float[]) H1.elements();
        final float[] h2 = (float[]) H2.elements();
        final float[] result = (float[]) Result.elements();
        int np = numberOfThreads;
        if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        int cC, rC, idx1, idx2;
                        float mag, h2e, h2o;
                        for (int r = firstRow; r < lastRow; r++) {
                            rC = (rows - r) % rows;
                            for (int c = 0; c < columns; c++) {
                                cC = (columns - c) % columns;
                                idx1 = c + columns * r;
                                idx2 = cC + columns * rC;
                                h2e = (h2[idx1] + h2[idx2]) / 2;
                                h2o = (h2[idx1] - h2[idx2]) / 2;
                                mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
                                float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
                                result[idx1] = (tmp / (mag + gammaScaled));
                            }
                        }

                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }
        } else {
            int cC, rC, idx1, idx2;
            float mag, h2e, h2o;
            for (int r = 0; r < rows; r++) {
                rC = (rows - r) % rows;
                for (int c = 0; c < columns; c++) {
                    cC = (columns - c) % columns;
                    idx1 = c + columns * r;
                    idx2 = cC + columns * rC;
                    h2e = (h2[idx1] + h2[idx2]) / 2;
                    h2o = (h2[idx1] - h2[idx2]) / 2;
                    mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
                    float tmp = h1[idx1] * h2e - h1[idx2] * h2o;
                    result[idx1] = (tmp / (mag + gammaScaled));
                }
            }
        }
    }

    private float energySum(FloatMatrix2D X, final int rows, final int columns, final int cOff, final int rOff) {
        float sumPixels = 0;
        final int rowStride = X.rowStride();
        final float[] elemsX = (float[]) X.elements();
        int np = numberOfThreads;
        if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            List<CompletableFuture<Float>> futures = new ArrayList<>();
            Float[] results = new Float[np];
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        float sum = 0;
                        for (int r = firstRow; r < lastRow; r++) {
                            for (int c = 0; c < columns; c++) {
                                sum += elemsX[c + cOff + rowStride * (r + rOff)];
                            }
                        }
                        return sum;
                    }, executor));
                }
            }
            try {
                for (int j = 0; j < np; j++) {
                    results[j] = futures.get(j).get();
                }
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Error applying wiener deconvolution", ex);
                for (int j = 0; j < np; j++) {
                    sumPixels += results[j];
                }
            }
        } else {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    sumPixels += elemsX[c + cOff + rowStride * (r + rOff)];
                }
            }
        }
        return sumPixels;
    }

    private int expandedSize(int psfSize, int bSize, ResizingType resizing) {
        int result = 0;
        int minimal = psfSize + bSize;
        switch (resizing) {
            case AUTO:
                int nextPowTwo;
                if (!ConcurrencyUtils.isPowerOf2(minimal)) {
                    nextPowTwo = ConcurrencyUtils.nextPow2(minimal);
                } else {
                    nextPowTwo = minimal;
                }
                if (nextPowTwo >= 1.5 * minimal) {
                    //use minimal padding
                    result = minimal;
                } else {
                    result = nextPowTwo;
                }
                break;
            case MINIMAL:
                result = minimal;
                break;
            case NEXT_POWER_OF_TWO:
                result = minimal;
                if (!ConcurrencyUtils.isPowerOf2(result)) {
                    result = ConcurrencyUtils.nextPow2(result);
                }
                break;
        }
        if (result < 4) {
            result = 4;
        }
        return result;
    }

    private float findMagMax(FloatMatrix2D H2) {
        final float[] h2 = (float[]) H2.elements();
        float magMax = 0;
        final int rows = H2.rows();
        final int columns = H2.columns();
        int np = numberOfThreads;
        if ((np > 1) && (rows * columns >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            List<CompletableFuture<Float>> futures = new ArrayList<>();
            Float[] results = new Float[np];
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        int cC, rC, idx1, idx2;
                        float max = 0;
                        float mag;
                        for (int r = firstRow; r < lastRow; r++) {
                            rC = (rows - r) % rows;
                            for (int c = 0; c < columns; c++) {
                                cC = (columns - c) % columns;
                                idx1 = c + columns * r;
                                idx2 = cC + columns * rC;
                                mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
                                if (mag > max)
                                    max = mag;
                            }
                        }
                        return max;
                    }, executor));
                }
            }

            try {
                for (int j = 0; j < np; j++) {
                    results[j] = futures.get(j).get();
                }
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Error applying wiener deconvolution", ex);
            }
            magMax = results[0];
            for (int j = 1; j < np; j++) {
                if (results[j] > magMax)
                    magMax = results[j];
            }
        } else {
            int cC, rC, idx1, idx2;
            float mag;
            for (int r = 0; r < rows; r++) {
                rC = (rows - r) % rows;
                for (int c = 0; c < columns; c++) {
                    cC = (columns - c) % columns;
                    idx1 = c + columns * r;
                    idx2 = cC + columns * rC;
                    mag = h2[idx1] * h2[idx1] + h2[idx2] * h2[idx2];
                    if (mag > magMax)
                        magMax = mag;
                }
            }
        }
        return magMax;
    }

    private void gaussianFilter(FloatMatrix2D X, final float[][] weights) throws ExecutionException, InterruptedException {
        final float[] elems = (float[]) X.elements();
        final int rows = X.rows();
        final int columns = X.columns();
        int np = numberOfThreads;
        if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int k = rows / np;
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        int idx = firstRow * columns;
                        for (int r = firstRow; r < lastRow; r++) {
                            for (int i = idx, c = 0; c < columns; c++) {
                                elems[i++] *= weights[1][r] * weights[0][c];
                            }
                            idx += columns;
                        }
                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }
        } else {
            int idx = 0;
            for (int r = 0; r < rows; r++) {
                for (int i = idx, c = 0; c < columns; c++) {
                    elems[i++] *= weights[1][r] * weights[0][c];
                }
                idx += columns;
            }
        }
    }

    private void gaussianFilterWithScaling(FloatMatrix2D X, final float[][] weights, final float scale) throws ExecutionException, InterruptedException {
        final float[] elems = (float[]) X.elements();
        final int rows = X.rows();
        final int columns = X.columns();
        int np = numberOfThreads;
        if ((np > 1) && (columns * rows >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int firstRow = j * k;
                    final int lastRow = (j == np - 1) ? rows : firstRow + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        int idx = firstRow * columns;
                        for (int r = firstRow; r < lastRow; r++) {
                            for (int i = idx, c = 0; c < columns; c++) {
                                elems[i++] *= weights[1][r] * weights[0][c] / scale;
                            }
                            idx += columns;
                        }

                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }
        } else {
            int idx = 0;
            for (int r = 0; r < rows; r++) {
                for (int i = idx, c = 0; c < columns; c++) {
                    elems[i++] *= weights[1][r] * weights[0][c] / scale;
                }
                idx += columns;
            }
        }
    }

    private float[][] gaussianWeights(final int rows, final int columns, final float filterX, final float filterY) {
        final float[][] weights = new float[2][];
        weights[0] = new float[columns];
        weights[1] = new float[rows];
        final float cc = (float) (columns / (filterX + 0.000001));
        final float rc = (float) (rows / (filterY + 0.000001));
        int np = numberOfThreads;
        if ((np > 1) && (Math.max(columns, rows) >= ConcurrencyUtils.getThreadsBeginN_1D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                int kcol = columns / np;
                int krow = rows / np;
                for (int j = 0; j < np; j++) {
                    final int firstCol = j * kcol;
                    final int lastCol = (j == np - 1) ? columns : firstCol + kcol;
                    final int firstRow = j * krow;
                    final int lastRow = (j == np - 1) ? rows : firstRow + krow;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        for (int c = firstCol; c < lastCol; c++) {
                            int cShifted = c;
                            if (cShifted > columns / 2)
                                cShifted = columns - cShifted;
                            float tmp = (cShifted / cc);
                            weights[0][c] = (float) Math.exp(-tmp * tmp);
                        }
                        for (int r = firstRow; r < lastRow; r++) {
                            int rShifted = r;
                            if (rShifted > rows / 2)
                                rShifted = rows - rShifted;
                            float tmp = (rShifted / rc);
                            weights[1][r] = (float) Math.exp(-tmp * tmp);
                        }
                    }, executor);
                }
                ConcurrencyUtils.waitForCompletion(futures);
            }
        } else {
            for (int c = 0; c < columns; c++) {
                int cShifted = c;
                if (cShifted > columns / 2)
                    cShifted = columns - cShifted;
                float tmp = (cShifted / cc);
                weights[0][c] = (float) Math.exp(-tmp * tmp);
            }
            for (int r = 0; r < rows; r++) {
                int rShifted = r;
                if (rShifted > rows / 2)
                    rShifted = rows - rShifted;
                float tmp = (rShifted / rc);
                weights[1][r] = (float) Math.exp(-tmp * tmp);
            }
        }
        return weights;
    }

    private float meanDelta(FloatMatrix2D B, FloatMatrix2D AX, FloatMatrix2D X, final float aSum) {
        float meanDelta = 0;
        final float[] elemsB = (float[]) B.elements();
        final float[] elemsAX = (float[]) AX.elements();
        final float[] elemsX = (float[]) X.elements();
        final int size = (int) B.size();
        int np = numberOfThreads;
        if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {

            List<CompletableFuture<Float>> futures = new ArrayList<>();
            Float[] results = new Float[np];
            int k = size / np;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int j = 0; j < np; j++) {
                    final int firstIdx = j * k;
                    final int lastIdx = (j == np - 1) ? size : firstIdx + k;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        float mean = 0;
                        float delta;
                        for (int i = firstIdx; i < lastIdx; i++) {
                            delta = (elemsB[i] - elemsAX[i] / aSum);
                            elemsX[i] += delta;
                            if (elemsX[i] < 0) {
                                elemsX[i] = 0;
                            } else {
                                mean += Math.abs(delta);
                            }
                        }
                        return mean;
                    }, executor));
                }
            }
            try {
                for (int j = 0; j < np; j++) {
                    results[j] = futures.get(j).get();
                }
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Error applying wiener deconvolution", ex);
            }
            for (int j = 0; j < np; j++) {
                meanDelta += results[j];
            }
        } else {
            float delta;
            for (int i = 0; i < size; i++) {
                delta = (elemsB[i] - elemsAX[i] / aSum);
                elemsX[i] += delta;
                if (elemsX[i] < 0) {
                    elemsX[i] = 0;
                } else {
                    meanDelta += Math.abs(delta);
                }
            }
        }
        return meanDelta;
    }

    private void toDB(FloatMatrix2D X, final float minDB) throws ExecutionException, InterruptedException {
        final float[] x = (float[]) X.elements();
        final float SCALE = (float) (10 / Math.log(10));
        final float minVal = (float) Math.exp(minDB / SCALE);
        int size = (int) X.size();
        int np = numberOfThreads;
        if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                CompletableFuture<?>[] futures = new CompletableFuture[np];
                int k = size / np;
                for (int j = 0; j < np; j++) {
                    final int firstIdx = j * k;
                    final int lastIdx = (j == np - 1) ? size : firstIdx + k;
                    futures[j] = CompletableFuture.runAsync(() -> {
                        for (int i = firstIdx; i < lastIdx; i++) {
                            if (x[i] > minVal)
                                x[i] = (float) (SCALE * Math.log(x[i]));
                            else
                                x[i] = minDB;
                        }
                    }, executor);
                }
                CompletableFuture.allOf(futures).get();
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (x[i] > minVal)
                    x[i] = (float) (SCALE * Math.log(x[i]));
                else
                    x[i] = minDB;
            }
        }
    }

    private float unDB(FloatMatrix2D X) {
        final float[] x = (float[]) X.elements();
        final float SCALE = (float) (10 / Math.log(10));
        final int size = (int) X.size();
        float min = Float.MAX_VALUE;
        int np = numberOfThreads;
        if ((np > 1) && (size >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            List<CompletableFuture<Float>> futures = new ArrayList<>();
            Float[] results = new Float[np];
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                int k = size / np;
                for (int j = 0; j < np; j++) {
                    final int firstIdx = j * k;
                    final int lastIdx = (j == np - 1) ? size : firstIdx + k;
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        float minimum = Float.MAX_VALUE;
                        for (int i = firstIdx; i < lastIdx; i++) {
                            if (x[i] < minimum)
                                minimum = x[i];
                            x[i] = (float) Math.exp(x[i] / SCALE);
                        }
                        return minimum;
                    }, executor));
                }
            }
            try {
                for (int j = 0; j < np; j++) {
                    results[j] = futures.get(j).get();
                }
            } catch (ExecutionException | InterruptedException ex) {
                log.error("Error applying wiener deconvolution", ex);
            }
            min = results[0];
            for (int j = 1; j < np; j++) {
                if (results[j] < min)
                    min = results[j];
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (x[i] < min)
                    min = x[i];
                x[i] = (float) Math.exp(x[i] / SCALE);
            }
        }
        return min;
    }

}
