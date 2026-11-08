# DAA Assignment 4 — Dynamic Programming and String Algorithms

City Search & Spell-Check: Edit Distance in three versions (recursive, table, two rows) and
string search with Naive and KMP, with work counters, a reproducible experiment, plots and a
report.

* Report (design card, plots, prediction vs result, short answers): [REPORT.md](REPORT.md)
* Raw data, one row per measured run: [results/results.csv](results/results.csv)
* Plots: [results/plots/](results/plots)

## Requirements

* JDK 17 or newer (developed with JDK 25)
* Maven 3.8 or newer (dependencies: JUnit 5 for tests, XChart for the PNG plots)

## Build and run the tests (one command)

```bash
mvn test
```

51 JUnit 5 tests: the reference values from section 7 of the assignment, empty strings, edge
cases (empty pattern throws, pattern longer than the text gives an empty list), 100 random
string pairs on which all three Edit Distance versions agree, 100 random texts on which Naive
and KMP return the same list, counter sanity checks and generator reproducibility.

## Run the experiment

```bash
mvn -q compile exec:java
```

This runs the whole experiment (about 2.5 minutes: 137 s on a MacBook Air M2 with 8 GB RAM), writes
`results/results.csv` and then draws the PNG plots into `results/plots/`. Two partial modes:

```bash
mvn -q compile exec:java -Dexec.args=experiment
```

```bash
mvn -q compile exec:java -Dexec.args=plots
```

The first only writes `results/results.csv`, the second only redraws the plots from the
existing CSV. All inputs come from a fixed seed (`20261108`), so the same command produces
the same inputs and the same counters; only `time_ms` depends on the machine.

### results.csv columns

| column | meaning |
|---|---|
| `experiment` | E1 (edit, n = 3..12), E2 (edit, n = 500..8000), S1 (search, vary n), S2 (search, vary m) |
| `family`, `algorithm` | `edit`: recursive / table / tworows, `search`: naive / kmp |
| `input` | edit: random / typo / disjoint; search: random / worst / allmatch |
| `n`, `m` | edit: lengths of a and b; search: text and pattern length |
| `run`, `time_ms` | measured run 1..5 (after 2 warm-up runs) and its wall-clock time |
| `result` | edit distance, or number of matches |
| `comparisons` | character comparisons (both families) |
| `subproblems`, `cells_allocated`, `max_depth` | edit only: D(i,j) values produced, DP ints allocated, recursion depth |
| `lps_comparisons`, `backsteps` | search only: KMP preprocessing comparisons, backward moves of the text pointer |

## Project layout

```
src/main/java/daa/edit/        EditDistance interface, Recursive/Table/TwoRow versions, EditCounters
src/main/java/daa/search/      StringSearch interface, NaiveSearch, KmpSearch (public lps), SearchCounters
src/main/java/daa/experiment/  InputGenerator (seeded), ExperimentRunner, PlotGenerator, Main
src/test/java/                 JUnit 5 tests
results/results.csv            all measured runs
results/plots/                 PNG plots used in the report
REPORT.md                      the report
```

## AI-use acknowledgement

An AI assistant (Claude by Anthropic, used through Claude Code) was used in this assignment.
It generated the source code, the JUnit tests, the experiment runner and plot generator, and
drafted REPORT.md and this README from the assignment text. All numbers and plots in the report
come from `results/results.csv`, produced by running the code in this repository.
