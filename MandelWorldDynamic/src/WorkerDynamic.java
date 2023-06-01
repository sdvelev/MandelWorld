import org.apache.commons.math3.complex.Complex;

public class WorkerDynamic implements java.lang.Runnable {
    private final int taskLocation;

    public WorkerDynamic(int taskLocation) {
        this.taskLocation = taskLocation;
    }

    private Complex calculateNext(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    private short generateIteration(Complex c) {
        Complex z = new Complex(0, 0);

        short currentIteration = -128;

        while (z.abs() <= 2 && currentIteration < MandelWorldDynamic.MAX_ITERATIONS) {
            z = calculateNext(z, c);
            currentIteration++;
        }

        return currentIteration;
    }

    @Override
    public void run() {
        int startFrom = taskLocation * MandelWorldDynamic.rowWidth;
        int endTo = taskLocation * MandelWorldDynamic.rowWidth + MandelWorldDynamic.rowWidth;
        for (int y = startFrom; y < endTo && y < MandelWorldDynamic.HEIGHT; y++) {
            for (int x = 0; x < MandelWorldDynamic.WIDTH; x++) {

                double pixel_x = MandelWorldDynamic.RE_START_POINT + ((double) x / MandelWorldDynamic.WIDTH) *
                    (MandelWorldDynamic.RE_END_POINT - MandelWorldDynamic.RE_START_POINT);

                double pixel_y = MandelWorldDynamic.IM_START_POINT + ((double) y / (MandelWorldDynamic.HEIGHT)) *
                    (MandelWorldDynamic.IM_END_POINT - MandelWorldDynamic.IM_START_POINT);

                Complex c = new Complex(pixel_x, pixel_y);
                MandelWorldDynamic.indexes[x][y] = (byte) generateIteration(c);
            }
        }
    }
}
