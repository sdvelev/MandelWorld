import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.*;

public class MandelWorldStatic {

    protected static int width = 3840, height = 2160;
    protected static int rows = 40, cols = 1, tasks = rows * cols;
    protected static int maxIterations = 1024;
    protected static double[] dims ={-0.6386,-0.5986,0.4456,0.4686};
    protected static int taskPixWidth = width / cols, taskPixHeight = height / rows;
    protected static String pathName = "MandelWorldStatic.png";
    protected static Thread[] workers;
    protected static int[][] pixels = new int[width][height];
    protected static int threads = 1;
    protected static boolean quiet = false, byCols = false;

    static private void addOptions(String[] args) {
        Options opt = new Options();
        opt.addOption("s", "size", true, "size of image (default: 3840x2160)");
        opt.addOption("r", "rect", true, "dimensions of area in the plane (default: -0.6386:-0.5986:0.4456:0.4686)");
        opt.addOption("t", "threads", true, "number of threads (default: 1)");
        opt.addOption("o", "out", true, "output path name (default: MandelWorldStatic.png)");
        opt.addOption("q", "quiet", false, "quiet mode (default: false)");
        opt.addOption("h", "help", false, "information about arguments (default: false)");
        opt.addOption("c", "cols", false, "decomposition by cols (default: false)");
        opt.addOption("g", "gran", true, "granularity (default: 1)");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(opt, args);
            if (cmd.hasOption("s")) {
                String[] sz = cmd.getOptionValue("s").split("x");
                try {
                    width = Integer.parseInt(sz[0]);
                    height = Integer.parseInt(sz[1]);
                    taskPixWidth = width / cols;
                    taskPixHeight = height / rows;
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
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            byCols = cmd.hasOption("c");

            if (cmd.hasOption("g")) {
                String gr = cmd.getOptionValue("g");
                try {
                    int gran = Integer.parseInt(gr);
                    if (byCols) {
                        cols = gran * threads;
                        rows = 1;
                    } else {
                        rows = gran * threads;
                        cols = 1;
                    }
                    tasks = rows * cols;
                    taskPixWidth = width / cols;
                    taskPixHeight = height / rows;
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    System.exit(5);
                }
            }

            if (cmd.hasOption("o")) {
                pathName = cmd.getOptionValue("o");
            }

            quiet = cmd.hasOption("q");

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("./runMe.sh [OPTIONS]", opt);
                System.exit(6);
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            System.exit(9);
        }
    }

    public static void main(String[] args) {
        addOptions(args);

        int[] colors = new int[maxIterations];

        for (int j = 0; j < maxIterations; ++j) {
         colors[j] = Color.HSBtoRGB(((100 + 1.7f*(float)Math.log(j)*(float)Math.sqrt(j)) / 256f), 0.77f,j / (j + 2.5f));
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphic = img.createGraphics();
        graphic.fillRect(0, 0, width, height);

        long start = System.currentTimeMillis();

        workers = new Thread[threads];
        for (int j = 1; j < threads; ++j) {
            WorkerStatic r = new WorkerStatic(j, quiet, maxIterations, width, height,
                    taskPixWidth, taskPixHeight, rows, cols, tasks, byCols, threads);
            Thread t = new Thread(r);
            t.start();
            workers[j] = t;
        }

        new WorkerStatic(0, quiet, maxIterations, width, height,
                taskPixWidth, taskPixHeight, rows, cols, tasks, byCols, threads).run();

        for (int j = 1; j < threads; ++j) {
            try {
                workers[j].join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (pixels[x][y] < maxIterations) {
                    img.setRGB(x, y, colors[pixels[x][y]]);
                    continue;
                }
                img.setRGB(x, y, Color.BLACK.getRGB());
            }
        }

        try {
            ImageIO.write(img, "png", new File(pathName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        PrintWriter out = new PrintWriter(System.out);
        out.printf("Total execution time is " + (end - start) + " ms.\n");

        out.flush();
        out.close();
    }
}
