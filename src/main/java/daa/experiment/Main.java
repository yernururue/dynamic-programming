package daa.experiment;

import java.nio.file.Path;

/**
 * Entry point.
 *
 * <pre>
 *   mvn -q compile exec:java                          # experiment + plots
 *   mvn -q compile exec:java -Dexec.args=experiment   # only results/results.csv
 *   mvn -q compile exec:java -Dexec.args=plots        # only re-draw results/plots/*.png
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        String mode = args.length > 0 ? args[0] : "all";
        Path csv = Path.of("results", "results.csv");
        Path plots = Path.of("results", "plots");

        switch (mode) {
            case "experiment" -> new ExperimentRunner(System.out).run(csv);
            case "plots" -> new PlotGenerator(csv, plots).generateAll();
            case "all" -> {
                new ExperimentRunner(System.out).run(csv);
                new PlotGenerator(csv, plots).generateAll();
            }
            default -> {
                System.err.println("usage: Main [all|experiment|plots]");
                System.exit(2);
            }
        }
    }
}
