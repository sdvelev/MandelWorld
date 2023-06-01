import org.apache.commons.math3.complex.Complex;

public class WorkerStatic implements java.lang.Runnable {

    private int indexThread;
    private boolean quiet, byCols;
    private int maxIterations;
    private int width, height;
    private double[] dims = MandelWorldStatic.dims;
    private int rows, cols;
    private int taskWidth, taskHeight, tasks, threads;

    public WorkerStatic(int indexThread, boolean quiet, int maxIterations,
                        int width, int height, int taskWidth, int taskHeight,
                        int rows, int cols, int tasks, boolean byCols, int threads) {
        this.indexThread = indexThread;
        this.quiet = quiet;
        this.maxIterations = maxIterations;
        this.width = width;
        this.height = height;
        this.rows = rows;
        this.cols = cols;
        this.taskWidth = taskWidth;
        this.taskHeight = taskHeight;
        this.tasks = tasks;
        this.byCols = byCols;
        this.threads = threads;
    }

    private Complex calculateNext(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private int generateIteration(Complex c) {
        Complex z = new Complex(0, 0);

        int currentIteration = 0;

        while (z.abs() <= 2 && currentIteration < maxIterations) {
            z = calculateNext(z, c);
            ++currentIteration;
        }
        return currentIteration;
    }

    public void byRows() {
        for (int task = indexThread; task < tasks; task += threads) {
            int p = (task % rows) * taskHeight;
            for (int x = p; x < p + taskHeight; ++x) {
                int q = (task / rows) * taskWidth;
                for (int y = q; y < q + taskWidth; ++y) {
                    double pixel_x = dims[2] + ((double) x / height) * (dims[3] - dims[2]),
                        pixel_y = dims[0] + ((double) y / width) * (dims[1] - dims[0]);

                    Complex c = new Complex(pixel_y, pixel_x);

                    MandelWorldStatic.pixels[y][x] = generateIteration(c);
                }
            }
        }
    }

    public void byCols() {
        for (int task = indexThread; task < tasks; task += threads) {
            int p = (task % cols) * taskWidth;
            for (int x = p; x < p + taskWidth; ++x) {
                int q = (task / cols) * taskHeight;
                for (int y = q; y < q + taskHeight; ++y) {
                    double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                        pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                    Complex c = new Complex(pixel_x, pixel_y);

                    MandelWorldStatic.pixels[x][y] = generateIteration(c);
                }
            }
        }
    }

    @Override
    public void run() {
        if (!quiet) {
            if (indexThread < 10) {
                System.out.println("Thread_0" + indexThread + " released.");
            } else {
                System.out.println("Thread_" + indexThread + " released.");
            }
        }

        long startTime = System.currentTimeMillis();

        if (byCols) {
            byCols();
        } else {
            byRows();
        }

        if (!quiet) {
            long endTime = System.currentTimeMillis();

            if (indexThread < 10) {
                System.out.println("Thread_0" + indexThread + " ready. Execution time was " + (endTime - startTime) + " ms.");
            } else {
                System.out.println("Thread_" + indexThread + " ready. Execution time was " + (endTime - startTime) + " ms.");
            }
        }
    }
}
