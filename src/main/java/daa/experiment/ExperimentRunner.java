package daa.experiment;

import daa.edit.EditCounters;
import daa.edit.EditDistance;
import daa.edit.RecursiveEditDistance;
import daa.edit.TableEditDistance;
import daa.edit.TwoRowEditDistance;
import daa.search.KmpSearch;
import daa.search.NaiveSearch;
import daa.search.SearchCounters;
import daa.search.StringSearch;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Runs the experiment described in the Experiment Design Card (REPORT.md, section 1) and
 * writes one CSV row per measured run.
 *
 * <ul>
 *   <li>E1 - Edit Distance, all three versions, n = 3..12, inputs random / typo / disjoint.</li>
 *   <li>E2 - Edit Distance, table and two rows, n = 500..8000, same inputs.</li>
 *   <li>E1L - limit probe added after the first run: recursive only, disjoint input, n = 13, 14,
 *       3 runs.</li>
 *   <li>S1 - Naive and KMP, text n = 125k..2M, m = 100, inputs random / worst / allmatch.</li>
 *   <li>S2 - Naive and KMP, n = 1M, m = 4..1024, inputs random / worst.</li>
 * </ul>
 * Each configuration gets {@value #WARMUP_RUNS} warm-up runs (not recorded) and
 * {@value #MEASURED_RUNS} measured runs. All algorithms of one configuration see the same input.
 */
public final class ExperimentRunner {

    public static final int WARMUP_RUNS = 2;
    public static final int MEASURED_RUNS = 5;

    static final int[] E1_SIZES = {3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
    static final int[] E2_SIZES = {500, 1000, 2000, 4000, 8000};
    static final List<String> EDIT_INPUTS = List.of("random", "typo", "disjoint");
    static final int[] E1L_SIZES = {13, 14};
    static final int LIMIT_PROBE_RUNS = 3;

    static final int[] S1_SIZES = {125_000, 250_000, 500_000, 1_000_000, 2_000_000};
    static final int S1_PATTERN = 100;
    static final int S2_TEXT = 1_000_000;
    static final int[] S2_PATTERNS = {4, 16, 64, 256, 1024};
    static final List<String> S1_INPUTS = List.of("random", "worst", "allmatch");
    static final List<String> S2_INPUTS = List.of("random", "worst");

    public static final String CSV_HEADER = "experiment,family,algorithm,input,n,m,run,time_ms,result,"
            + "comparisons,subproblems,cells_allocated,max_depth,lps_comparisons,backsteps";

    private final PrintStream log;
    private PrintWriter csv;

    public ExperimentRunner(PrintStream log) {
        this.log = log;
    }

    public void run(Path csvFile) throws IOException {
        Files.createDirectories(csvFile.toAbsolutePath().getParent());
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8))) {
            this.csv = writer;
            csv.println(CSV_HEADER);
            long start = System.nanoTime();

            log.println("JIT warm-up ...");
            warmUp();

            EditDistance recursive = new RecursiveEditDistance();
            EditDistance table = new TableEditDistance();
            EditDistance twoRows = new TwoRowEditDistance();
            runEdit("E1", E1_SIZES, EDIT_INPUTS, List.of(recursive, table, twoRows), WARMUP_RUNS, MEASURED_RUNS);
            runEdit("E2", E2_SIZES, EDIT_INPUTS, List.of(table, twoRows), WARMUP_RUNS, MEASURED_RUNS);
            // Added after the first run (see REPORT.md 1.1): where does recursion really stop?
            // Worst-case input only, 3 runs and no extra warm-up, because one run takes seconds.
            runEdit("E1L", E1L_SIZES, List.of("disjoint"), List.of(recursive), 0, LIMIT_PROBE_RUNS);

            List<StringSearch> searches = List.of(new NaiveSearch(), new KmpSearch());
            for (String input : S1_INPUTS) {
                for (int n : S1_SIZES) {
                    runSearch("S1", input, n, S1_PATTERN, searches);
                }
            }
            for (String input : S2_INPUTS) {
                for (int m : S2_PATTERNS) {
                    runSearch("S2", input, S2_TEXT, m, searches);
                }
            }
            log.printf(Locale.ROOT, "Done in %.1f s, results written to %s%n",
                    (System.nanoTime() - start) / 1e9, csvFile);
        } finally {
            this.csv = null;
        }
    }

    // ------------------------------------------------------------------ Edit Distance

    private void runEdit(String experiment, int[] sizes, List<String> inputs, List<EditDistance> algorithms,
                         int warmupRuns, int measuredRuns) {
        for (String input : inputs) {
            for (int n : sizes) {
                InputGenerator.Pair p = new InputGenerator(InputGenerator.seedFor(experiment, input, n, 0))
                        .editPair(input, n);
                Integer expected = null;
                for (EditDistance algo : algorithms) {
                    for (int w = 0; w < warmupRuns; w++) {
                        algo.distance(p.a(), p.b());
                    }
                    double[] times = new double[measuredRuns];
                    for (int run = 1; run <= measuredRuns; run++) {
                        long t0 = System.nanoTime();
                        int d = algo.distance(p.a(), p.b());
                        long t1 = System.nanoTime();
                        times[run - 1] = (t1 - t0) / 1e6;

                        if (expected == null) {
                            expected = d;
                        } else if (expected != d) {
                            throw new IllegalStateException(algo.name() + " returned " + d
                                    + " but expected " + expected + " for " + experiment + "/" + input + "/n=" + n);
                        }
                        EditCounters c = algo.counters();
                        csv.printf(Locale.ROOT, "%s,edit,%s,%s,%d,%d,%d,%.4f,%d,%d,%d,%d,%d,,%n",
                                experiment, algo.name(), input, p.a().length(), p.b().length(), run,
                                times[run - 1], d, c.comparisons, c.subproblems, c.cellsAllocated, c.maxDepth);
                    }
                    csv.flush();
                    log.printf(Locale.ROOT, "%-3s %-9s %-9s n=%-5d median=%10.4f ms  subproblems=%d%n",
                            experiment, algo.name(), input, n, median(times), algo.counters().subproblems);
                }
            }
        }
    }

    // ------------------------------------------------------------------ String search

    private void runSearch(String experiment, String input, int n, int m, List<StringSearch> algorithms) {
        InputGenerator.SearchCase sc = new InputGenerator(InputGenerator.seedFor(experiment, input, n, m))
                .searchCase(input, n, m);
        List<Integer> expected = null;
        for (StringSearch algo : algorithms) {
            for (int w = 0; w < WARMUP_RUNS; w++) {
                algo.search(sc.text(), sc.pattern());
            }
            double[] times = new double[MEASURED_RUNS];
            for (int run = 1; run <= MEASURED_RUNS; run++) {
                long t0 = System.nanoTime();
                List<Integer> found = algo.search(sc.text(), sc.pattern());
                long t1 = System.nanoTime();
                times[run - 1] = (t1 - t0) / 1e6;

                if (expected == null) {
                    expected = found;
                } else if (!expected.equals(found)) {
                    throw new IllegalStateException(algo.name() + " disagrees on " + experiment
                            + "/" + input + "/n=" + n + "/m=" + m);
                }
                SearchCounters c = algo.counters();
                csv.printf(Locale.ROOT, "%s,search,%s,%s,%d,%d,%d,%.4f,%d,%d,,,,%d,%d%n",
                        experiment, algo.name(), input, n, m, run, times[run - 1], found.size(),
                        c.comparisons, c.lpsComparisons, c.backsteps);
            }
            csv.flush();
            log.printf(Locale.ROOT, "%-3s %-9s %-9s n=%-8d m=%-5d median=%10.4f ms  comparisons=%d%n",
                    experiment, algo.name(), input, n, m, median(times), algo.counters().comparisons);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Lets the JIT compile the hot loops before anything is measured. Nothing is recorded. */
    private static void warmUp() {
        EditDistance[] edits = {new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance()};
        StringSearch[] searches = {new NaiveSearch(), new KmpSearch()};
        InputGenerator g = new InputGenerator(1);
        for (int k = 0; k < 30; k++) {
            InputGenerator.Pair small = g.editPair(k % 2 == 0 ? "random" : "disjoint", 8);
            InputGenerator.Pair large = g.editPair("random", 300);
            edits[0].distance(small.a(), small.b());
            edits[1].distance(large.a(), large.b());
            edits[2].distance(large.a(), large.b());
            for (String type : List.of("random", "worst", "allmatch")) {
                InputGenerator.SearchCase sc = g.searchCase(type, 50_000, 20);
                searches[0].search(sc.text(), sc.pattern());
                searches[1].search(sc.text(), sc.pattern());
            }
        }
    }

    static double median(double[] values) {
        double[] v = values.clone();
        Arrays.sort(v);
        int mid = v.length / 2;
        return v.length % 2 == 1 ? v[mid] : (v[mid - 1] + v[mid]) / 2.0;
    }
}
