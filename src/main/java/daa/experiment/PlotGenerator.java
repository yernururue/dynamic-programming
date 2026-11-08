package daa.experiment;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Reads results/results.csv, takes the median time of the measured runs of every configuration
 * and draws the PNG plots into results/plots/. Each PNG has a title, axis titles with units and
 * a one-sentence takeaway printed under the chart.
 */
public final class PlotGenerator {

    /** One configuration = one point on a plot. */
    record Key(String experiment, String algorithm, String input, int n, int m) { }

    /** Median time of the runs plus the (deterministic) counters of the configuration. */
    static final class Stat {
        final List<Double> times = new ArrayList<>();
        double medianMs;
        long result;
        long comparisons;
        long subproblems;
        long cellsAllocated;
        long maxDepth;
        long lpsComparisons;
        long backsteps;
    }

    private static final Color BLUE = new Color(0x1F77B4);
    private static final Color ORANGE = new Color(0xE8710A);
    private static final Color GREEN = new Color(0x2CA02C);
    private static final Color RED = new Color(0xD62728);
    private static final Color PURPLE = new Color(0x7B4FB8);
    private static final Color GRAY = new Color(0x555555);

    private static final Map<String, Color> INPUT_COLOR = Map.of(
            "random", BLUE, "typo", GREEN, "disjoint", RED,
            "worst", RED, "allmatch", PURPLE);

    private static final Map<String, String> INPUT_LABEL = Map.of(
            "random", "random",
            "typo", "typo (similar words)",
            "disjoint", "disjoint (worst case)",
            "worst", "worst: A..A vs A..AB",
            "allmatch", "allmatch: A..A vs A..A");

    private final Path csvFile;
    private final Path outDir;
    private final Map<Key, Stat> stats = new LinkedHashMap<>();

    public PlotGenerator(Path csvFile, Path outDir) {
        this.csvFile = csvFile;
        this.outDir = outDir;
    }

    public void generateAll() throws IOException {
        load();
        Files.createDirectories(outDir);
        p1EditSubproblems();
        p2EditTimeSmallN();
        p3EditDpTime();
        p4EditNsPerCell();
        p5SearchComparisons();
        p6SearchRatioVsM();
        p7SearchTimeVsN();
        System.out.println("Plots written to " + outDir);
    }

    // ------------------------------------------------------------------ loading

    void load() throws IOException {
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        String[] header = lines.get(0).split(",", -1);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            col.put(header[i], i);
        }
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(",", -1);
            Key key = new Key(f[col.get("experiment")], f[col.get("algorithm")], f[col.get("input")],
                    Integer.parseInt(f[col.get("n")]), Integer.parseInt(f[col.get("m")]));
            Stat s = stats.computeIfAbsent(key, k -> new Stat());
            s.times.add(Double.parseDouble(f[col.get("time_ms")]));
            s.result = parseLong(f[col.get("result")]);
            s.comparisons = parseLong(f[col.get("comparisons")]);
            s.subproblems = parseLong(f[col.get("subproblems")]);
            s.cellsAllocated = parseLong(f[col.get("cells_allocated")]);
            s.maxDepth = parseLong(f[col.get("max_depth")]);
            s.lpsComparisons = parseLong(f[col.get("lps_comparisons")]);
            s.backsteps = parseLong(f[col.get("backsteps")]);
        }
        for (Stat s : stats.values()) {
            double[] t = s.times.stream().mapToDouble(Double::doubleValue).toArray();
            s.medianMs = ExperimentRunner.median(t);
        }
    }

    private static long parseLong(String s) {
        return s.isEmpty() ? 0 : Long.parseLong(s);
    }

    /** Points of one (experiment, algorithm, input) line, sorted by x. */
    private double[][] points(String experiment, String algorithm, String input,
                              ToDoubleFunction<Map.Entry<Key, Stat>> x,
                              ToDoubleFunction<Map.Entry<Key, Stat>> y) {
        List<double[]> pts = new ArrayList<>();
        for (Map.Entry<Key, Stat> e : stats.entrySet()) {
            Key k = e.getKey();
            if (k.experiment().equals(experiment) && k.algorithm().equals(algorithm) && k.input().equals(input)) {
                pts.add(new double[]{x.applyAsDouble(e), y.applyAsDouble(e)});
            }
        }
        pts.sort((p, q) -> Double.compare(p[0], q[0]));
        double[] xs = pts.stream().mapToDouble(p -> p[0]).toArray();
        double[] ys = pts.stream().mapToDouble(p -> p[1]).toArray();
        return new double[][]{xs, ys};
    }

    /** Recursive points of E1 plus the limit probe E1L (which exists only for some inputs). */
    private double[][] recursivePoints(String input, ToDoubleFunction<Map.Entry<Key, Stat>> y) {
        double[][] a = points("E1", "recursive", input, PlotGenerator::n, y);
        double[][] b = points("E1L", "recursive", input, PlotGenerator::n, y);
        double[] xs = new double[a[0].length + b[0].length];
        double[] ys = new double[xs.length];
        System.arraycopy(a[0], 0, xs, 0, a[0].length);
        System.arraycopy(b[0], 0, xs, a[0].length, b[0].length);
        System.arraycopy(a[1], 0, ys, 0, a[1].length);
        System.arraycopy(b[1], 0, ys, a[1].length, b[1].length);
        return new double[][]{xs, ys};
    }

    private Stat stat(String experiment, String algorithm, String input, int n, int m) {
        return stats.get(new Key(experiment, algorithm, input, n, m));
    }

    private static double n(Map.Entry<Key, Stat> e) {
        return e.getKey().n();
    }

    // ------------------------------------------------------------------ Edit Distance plots

    private void p1EditSubproblems() throws IOException {
        XYChart c = chart("P1. Edit Distance: subproblems solved vs string length",
                "string length n (characters)", "subproblems = recursive calls or filled cells (log scale)");
        c.getStyler().setYAxisLogarithmic(true);
        for (String input : List.of("disjoint", "random", "typo")) {
            add(c, "recursive, " + INPUT_LABEL.get(input),
                    recursivePoints(input, e -> e.getValue().subproblems),
                    INPUT_COLOR.get(input), SeriesMarkers.CIRCLE, SeriesLines.SOLID);
        }
        add(c, "table and two rows, every input: (n+1)(m+1)",
                points("E1", "table", "random", PlotGenerator::n, e -> e.getValue().subproblems),
                GRAY, SeriesMarkers.SQUARE, SeriesLines.DASH_DASH);
        save(c, "p1_edit_subproblems_small_n.png",
                "Recursive calls grow about 5.6x per extra character on the disjoint (worst-case) input "
                        + "(377 million calls at n = 12, 11.9 billion at n = 14) and almost as fast on random words, "
                        + "while the table always fills (n+1)(m+1) cells (169 at n = 12); similar words (typo) are much "
                        + "cheaper but jump up and down with the position of the typo.");
    }

    private void p2EditTimeSmallN() throws IOException {
        XYChart c = chart("P2. Edit Distance: running time for short words",
                "string length n (characters)", "median time of 5 runs (ms, log scale)");
        c.getStyler().setYAxisLogarithmic(true);
        for (String input : List.of("disjoint", "random", "typo")) {
            add(c, "recursive, " + INPUT_LABEL.get(input),
                    recursivePoints(input, e -> e.getValue().medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.CIRCLE, SeriesLines.SOLID);
        }
        add(c, "table, random", points("E1", "table", "random", PlotGenerator::n, e -> e.getValue().medianMs),
                GRAY, SeriesMarkers.SQUARE, SeriesLines.DASH_DASH);
        add(c, "two rows, random", points("E1", "tworows", "random", PlotGenerator::n, e -> e.getValue().medianMs),
                ORANGE, SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        save(c, "p2_edit_time_small_n.png",
                "Recursive time follows its call count (about x5.6 per character): 0.75 s at n = 12, "
                        + "4.2 s at n = 13 and 23.5 s at n = 14 on the worst input, while both DP versions stay "
                        + "below one microsecond for all these sizes.");
    }

    private void p3EditDpTime() throws IOException {
        XYChart c = chart("P3. Edit Distance: table vs two rows for long strings",
                "string length n = m (characters)", "median time of 5 runs (ms)");
        for (String input : List.of("random", "typo", "disjoint")) {
            add(c, "table, " + INPUT_LABEL.get(input),
                    points("E2", "table", input, PlotGenerator::n, e -> e.getValue().medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.SQUARE, SeriesLines.SOLID);
            add(c, "two rows, " + INPUT_LABEL.get(input),
                    points("E2", "tworows", input, PlotGenerator::n, e -> e.getValue().medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        }
        save(c, "p3_edit_dp_time_large_n.png",
                "Both DP versions grow about x4 per doubling of n (quadratic) on every input; at n = 8000 "
                        + "two rows is about 1.5x faster than the full table (139 vs 203 ms on random words) while "
                        + "allocating 16,002 instead of 64 million int cells.");
    }

    private void p4EditNsPerCell() throws IOException {
        XYChart c = chart("P4. Edit Distance: time per DP cell (ratio)",
                "string length n = m (characters)", "median time / filled cells (ns per cell)");
        c.getStyler().setYAxisMin(0.0);
        c.getStyler().setLegendPosition(Styler.LegendPosition.InsideSE);
        for (String input : List.of("random", "typo", "disjoint")) {
            add(c, "table, " + INPUT_LABEL.get(input),
                    points("E2", "table", input, PlotGenerator::n,
                            e -> e.getValue().medianMs * 1e6 / e.getValue().subproblems),
                    INPUT_COLOR.get(input), SeriesMarkers.SQUARE, SeriesLines.SOLID);
            add(c, "two rows, " + INPUT_LABEL.get(input),
                    points("E2", "tworows", input, PlotGenerator::n,
                            e -> e.getValue().medianMs * 1e6 / e.getValue().subproblems),
                    INPUT_COLOR.get(input), SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        }
        save(c, "p4_edit_dp_ns_per_cell.png",
                "Two rows costs a flat 2.2 ns per cell (2.6 ns on disjoint input), so its time is exactly "
                        + "proportional to n*m; the table pays more per cell and is noisier (2.6-4.6 ns) because it "
                        + "must allocate and zero up to 256 MB.");
    }

    // ------------------------------------------------------------------ String search plots

    private void p5SearchComparisons() throws IOException {
        XYChart c = chart("P5. String search: comparisons per text character (m = 100)",
                "text length n (characters)", "comparisons / n (ratio, log scale)");
        c.getStyler().setYAxisLogarithmic(true);
        c.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        for (String input : List.of("worst", "allmatch", "random")) {
            add(c, "naive, " + INPUT_LABEL.get(input),
                    points("S1", "naive", input, PlotGenerator::n, e -> (double) e.getValue().comparisons / n(e)),
                    INPUT_COLOR.get(input), SeriesMarkers.CIRCLE, SeriesLines.SOLID);
            add(c, "KMP, " + INPUT_LABEL.get(input),
                    points("S1", "kmp", input, PlotGenerator::n, e -> (double) e.getValue().comparisons / n(e)),
                    INPUT_COLOR.get(input), SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        }
        save(c, "p5_search_comparisons_per_char.png",
                "Every line is flat, so all costs are linear in n, but at very different levels: Naive needs "
                        + "100 comparisons per text character on both A..A inputs (the two lines coincide), KMP needs 2 "
                        + "(worst) or 1 (allmatch), and on random text both need about 1.04.");
    }

    private void p6SearchRatioVsM() throws IOException {
        XYChart c = chart("P6. String search: how many times Naive costs more than KMP (n = 1,000,000)",
                "pattern length m (characters, log scale)", "Naive / KMP (ratio, log scale)");
        c.getStyler().setXAxisLogarithmic(true);
        c.getStyler().setYAxisLogarithmic(true);
        for (String input : List.of("worst", "random")) {
            add(c, "comparisons ratio, " + INPUT_LABEL.get(input),
                    ratio(input, s -> s.comparisons + s.lpsComparisons),
                    INPUT_COLOR.get(input), SeriesMarkers.CIRCLE, SeriesLines.SOLID);
            add(c, "time ratio, " + INPUT_LABEL.get(input),
                    ratio(input, s -> s.medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        }
        save(c, "p6_search_ratio_vs_m.png",
                "On the worst input the Naive/KMP gap grows in proportion to the pattern length (2x at m = 4, "
                        + "about 510x at m = 1024, i.e. about m/2) in comparisons and in time alike; on random text the "
                        + "ratio stays at about 1 for every m.");
    }

    /** Naive / KMP for experiment S2 as a function of m (KMP counted with its LPS work). */
    private double[][] ratio(String input, ToDoubleFunction<Stat> metric) {
        double[] ms = Arrays.stream(ExperimentRunner.S2_PATTERNS).asDoubleStream().toArray();
        double[] r = new double[ms.length];
        for (int i = 0; i < ms.length; i++) {
            int m = ExperimentRunner.S2_PATTERNS[i];
            Stat naive = stat("S2", "naive", input, ExperimentRunner.S2_TEXT, m);
            Stat kmp = stat("S2", "kmp", input, ExperimentRunner.S2_TEXT, m);
            r[i] = metric.applyAsDouble(naive) / metric.applyAsDouble(kmp);
        }
        return new double[][]{ms, r};
    }

    private void p7SearchTimeVsN() throws IOException {
        XYChart c = chart("P7. String search: running time vs text length (m = 100)",
                "text length n (characters)", "median time of 5 runs (ms, log scale)");
        c.getStyler().setYAxisLogarithmic(true);
        c.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        for (String input : List.of("worst", "allmatch", "random")) {
            add(c, "naive, " + INPUT_LABEL.get(input),
                    points("S1", "naive", input, PlotGenerator::n, e -> e.getValue().medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.CIRCLE, SeriesLines.SOLID);
            add(c, "KMP, " + INPUT_LABEL.get(input),
                    points("S1", "kmp", input, PlotGenerator::n, e -> e.getValue().medianMs),
                    INPUT_COLOR.get(input), SeriesMarkers.DIAMOND, SeriesLines.DASH_DASH);
        }
        save(c, "p7_search_time_vs_n.png",
                "Times confirm the counters: Naive on the A..A inputs is about 50x slower than KMP; KMP on "
                        + "allmatch is slower than KMP on worst despite fewer comparisons, because it has to store "
                        + "about 2 million matches.");
    }

    // ------------------------------------------------------------------ chart helpers

    private static XYChart chart(String title, String xTitle, String yTitle) {
        XYChart c = new XYChartBuilder().width(1000).height(620)
                .title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
        XYStyler s = c.getStyler();
        s.setChartBackgroundColor(Color.WHITE);
        s.setPlotBackgroundColor(Color.WHITE);
        s.setPlotBorderColor(new Color(0xBBBBBB));
        s.setPlotGridLinesColor(new Color(0xE3E3E3));
        s.setChartTitleBoxVisible(false);
        s.setChartTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        s.setAxisTitleFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        s.setAxisTickLabelsFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        s.setLegendFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        s.setLegendPosition(Styler.LegendPosition.InsideNW);
        s.setLegendBorderColor(new Color(0xCCCCCC));
        s.setMarkerSize(8);
        s.setXAxisDecimalPattern("#,##0.###");
        s.setYAxisDecimalPattern("#,##0.####");
        s.setChartPadding(14);
        return c;
    }

    private static void add(XYChart c, String name, double[][] xy, Color color, Marker marker, BasicStroke line) {
        XYSeries s = c.addSeries(name, xy[0], xy[1]);
        s.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        s.setLineColor(color);
        s.setMarkerColor(color);
        s.setMarker(marker);
        s.setLineStyle(line);
    }

    /** Renders the chart and prints the one-sentence takeaway under it. */
    private void save(XYChart chart, String fileName, String takeaway) throws IOException {
        BufferedImage plot = BitmapEncoder.getBufferedImage(chart);
        int width = plot.getWidth();
        Font font = new Font(Font.SANS_SERIF, Font.ITALIC, 15);

        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D pg = probe.createGraphics();
        FontMetrics fm = pg.getFontMetrics(font);
        List<String> lines = wrap("Takeaway: " + takeaway, fm, width - 60);
        pg.dispose();

        int lineHeight = fm.getHeight();
        int height = plot.getHeight() + 16 + lines.size() * lineHeight + 14;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(plot, 0, 0, null);
        g.setColor(new Color(0x333333));
        g.setFont(font);
        int y = plot.getHeight() + 10 + fm.getAscent();
        for (String l : lines) {
            g.drawString(l, 30, y);
            y += lineHeight;
        }
        g.dispose();
        ImageIO.write(out, "png", outDir.resolve(fileName).toFile());
    }

    private static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(candidate) > maxWidth && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }
}
