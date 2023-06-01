import org.apache.commons.cli.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class MandelWorldDynamic {
    static int WIDTH = 3840, HEIGHT = 2160, MAX_ITERATIONS = 1024;
    static double RE_START_POINT = -0.6386, RE_END_POINT = -0.5986, IM_START_POINT = 0.4456, IM_END_POINT = 0.4686;
    static byte[][] indexes;
    static int numThreads = 1, granularity = 1, rows, rowWidth;
    static String imageName = "MandelWorldDynamic.png";

    protected static long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    static void insertOptions(String[] args) {
        Options options = new Options();
        options.addOption("r", "rect", true,
            "dimension of area in the complex plane (default: -0.6386:-0.5986:0.4456:0.4686)");
        options.addOption("t", "threads", true, "number of threads (default: 1)");
        options.addOption("o", "out", true, "outputh path name (default: MandelWorldDynamic.png)");
        options.addOption("s", "size", true, "size of image (default: 3840x2160)");
        options.addOption("h", "help", false, "information about arguments (default: false)");
        options.addOption("g", "granularity", true, "granularity (how many tasks per thread) (default: 1)");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMe.sh [OPTIONS]", options);
                System.exit(0);
            }

            if (cmd.hasOption("t")) {
                try {
                    numThreads = Integer.parseInt(cmd.getOptionValue("t"));
                } catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
            }

            if (cmd.hasOption("o")) {
                imageName = cmd.getOptionValue("o");
            }

            if (cmd.hasOption("r")) {
                String[] rectPoints = cmd.getOptionValue("r").split(":");
                try {
                    RE_START_POINT = Float.parseFloat(rectPoints[0]);
                    RE_END_POINT = Float.parseFloat(rectPoints[1]);
                    IM_START_POINT = Float.parseFloat(rectPoints[2]);
                    IM_END_POINT = Float.parseFloat(rectPoints[3]);
                } catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Array index out of bounds at: " + e.getMessage() +
                        ". Please provide 4 values to the '-r' option.");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("s")) {
                String[] sizeParams = cmd.getOptionValue("s").split("x");
                try {
                    WIDTH = Integer.parseInt(sizeParams[0]);
                    HEIGHT = Integer.parseInt(sizeParams[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Array index out of bounds at: " + e.getMessage() +
                        ". Please provide 2 values to the '-s' option.");
                    System.exit(1);
                }
            }

            if (cmd.hasOption("g")) {
                try {
                    granularity = Integer.parseInt(cmd.getOptionValue("g"));
                } catch (NumberFormatException e) {
                    System.out.println("Unexpected exception: " + e.getMessage());
                    System.exit(1);
                }
            }
        } catch (ParseException exp) {
            System.out.println("Unexpected exception:" + exp.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        insertOptions(args);
        indexes = new byte[WIDTH][HEIGHT];
        rows = numThreads * granularity;
        rowWidth = (int) Math.ceil((float) HEIGHT / rows);

        int[] colors = new int[MAX_ITERATIONS];
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            colors[i] = Color.HSBtoRGB((100f + i * 2f) / 256f, 0.87f, i / (i + 2.5f));
        }

        colors[127] = Color.BLACK.getRGB();

        BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = bi.createGraphics();
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        long startTime = getTimeInMillis();

        ExecutorService pool = Executors.newFixedThreadPool(numThreads - 1);

        WorkerDynamic[] tasks = new WorkerDynamic[rows];
        for (int i = 0; i < rows; i++) {
            tasks[i] = new WorkerDynamic(i);
        }

        for (int i = 0; i < rows; i++) {
            pool.execute(tasks[i]);
        }

        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = getTimeInMillis();

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (indexes[x][y] < 0) {
                    indexes[x][y] += 128;
                }
                bi.setRGB(x, y, colors[indexes[x][y]]);
            }
        }

        try {
            ImageIO.write(bi, "png", new File(imageName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Total execution time is: " + (endTime - startTime) + " ms.");
    }
}
