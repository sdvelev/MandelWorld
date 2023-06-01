import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.cli.*;

public class MandelWorldDynamicCustomThreadPool {

    protected static int width = 3840, height = 2160;
    protected static double[] dims = {-0.6386,-0.5986,0.4456,0.4686};
    protected static int threads = 1, tasks = 1, gran = 1, maxIterations = 1024;
    protected static int[][] pixels = new int[width][height];
    protected static String pathName = "MandelWorldDynamicCustomThreadPool.png";
    protected static boolean quiet = false, byCols = false;
    protected static int numberOfFinishedTasks = 0;
    protected static synchronized void increaseFinishedTasks() {
        ++numberOfFinishedTasks;
    }

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static public void addOptions(String[] args) {
        Options opt = new Options();
        opt.addOption("s", "size", true, "size of image (default: 3840x2160)");
        opt.addOption("r", "rect", true, "dimensions of area in the complex plane (-0.6386:-0.5986:0.4456:0.4686)");
        opt.addOption("t", "threads", true, "number of threads (default: 1)");
        opt.addOption("o", "out", true, "output path name (MandelWorldDynamicCustomThreadPool.png)");
        opt.addOption("q", "quiet", false, "quiet mode (default: false)");
        opt.addOption("h", "help", false, "information about arguments (default: false)");
        opt.addOption("g", "gran", true, "granularity (default: 1)");
        opt.addOption("c", "cols", false, "decomposition by cols (default: false)");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(opt, args);
            if (cmd.hasOption("s")) {
                String[] size = cmd.getOptionValue("s").split("x");
                try {
                    width = Integer.parseInt(size[0]);
                    height = Integer.parseInt(size[1]);
                    pixels = new int[width][height];
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(e.getMessage());
                    System.exit(2);
                }
            }

            if (cmd.hasOption("r")) {
                String[] dim = cmd.getOptionValue("r").split(":");
                try {
                    for (int j = 0; j < dim.length; ++j) {
                        dims[j] = Float.parseFloat(dim[j]);
                    }
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(3);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println(e.getMessage());
                    System.exit(4);
                }
            }

            if (cmd.hasOption("t")) {
                String workers = cmd.getOptionValue("t");
                try {
                    threads = Integer.parseInt(workers);
                    tasks = gran * threads;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            if (cmd.hasOption("g")) {
                String g = cmd.getOptionValue("g");
                try {
                    gran = Integer.parseInt(g);
                    tasks = gran * threads;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(6);
                }
            }

            if (cmd.hasOption("o")) {
                pathName = cmd.getOptionValue("o");
            }

            quiet = cmd.hasOption("q");
            byCols = cmd.hasOption("c");

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMe.sh [OPTIONS]", opt);
                System.exit(7);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(8);
        }
    }

    static private Complex calculateNext(Complex z, Complex c) {
        return z.multiply(z).add(c);
    }

    static private int generateIteration(Complex c) {
        Complex z = new Complex(0, 0);

        int currentIteration = 0;

        while (z.abs() <= 2 && currentIteration < maxIterations) {
            z = calculateNext(z, c);
            ++currentIteration;
        }
        return currentIteration;
    }

    static private void byRows(int current, int part) {
        for (int y = current; y < current + part && y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                        pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                Complex c = new Complex(pixel_x, pixel_y);

                pixels[x][y] = generateIteration(c);
            }
        }
    }

    static private void byCols(int current, int part) {
        for (int x = current; x < current + part && x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                double pixel_x = dims[0] + ((double) x / width) * (dims[1] - dims[0]),
                        pixel_y = dims[2] + ((double) y / height) * (dims[3] - dims[2]);

                Complex c = new Complex(pixel_x, pixel_y);

                pixels[x][y] = generateIteration(c);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        addOptions(args);

        int[] colors = new int[maxIterations];
        for (int i = 0; i < maxIterations; ++i) {
            colors[i] = Color.HSBtoRGB((100 + 1.7f * i) / 256, 0.77f, i / (i + 2.5f));
        }

        BufferedImage myImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = myImage.createGraphics();
        g2.fillRect(0, 0, width, height);

        long startTime = getTimeInMillis();

        ThreadPoolCustom threadPoolCustom = new ThreadPoolCustom(threads - 1, tasks);

        int div = tasks;
        int partRows = height / div + (height % div == 0 ? 0 : 1);
        int partCols = width / div + (width % div == 0 ? 0 : 1);
        final int part = byCols ? partCols : partRows;
        for (int k = 0; k < div; ++k) {
            final int current = k * part;
            threadPoolCustom.execute(() -> {
                if (byCols) {
                    byCols(current, part);
                } else {
                    byRows(current, part);
                }
                increaseFinishedTasks();
            });
        }

        threadPoolCustom.waitUntilAllTasksFinished();
        threadPoolCustom.stop(quiet);

        try {
            while (numberOfFinishedTasks < tasks) {
                Thread.sleep(20);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long endTime = getTimeInMillis();

        threadPoolCustom.killThreads();

        for (int i = 0; i < width; ++i) {
            for (int j = 0; j < height; ++j) {
                if (pixels[i][j] == maxIterations) {
                    myImage.setRGB(i, j, Color.BLACK.getRGB());
                } else {
                    myImage.setRGB(i, j, colors[pixels[i][j]]);
                }
            }
        }

        try {
            ImageIO.write(myImage, "png", new File(pathName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total execution time is " + (endTime - startTime) + "ms.\n");
    }
}
