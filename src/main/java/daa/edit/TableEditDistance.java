package daa.edit;

import java.util.Objects;

/**
 * Version B: bottom-up table of size (n+1) x (m+1). Time O(n*m), memory O(n*m).
 *
 * <p>State:      d[i][j] = edit distance between the prefixes a[0..i) and b[0..j).
 * <br>Transition: d[i][j] = d[i-1][j-1] if a[i-1] == b[j-1], otherwise
 *                 1 + min(d[i-1][j], d[i][j-1], d[i-1][j-1]); rows are filled top to bottom.
 * <br>Base case:  d[0][j] = j and d[i][0] = i. Answer: d[n][m].
 *
 * <p>Each subproblem is computed exactly once, so the work is (n+1)*(m+1) cells
 * no matter what the strings contain.
 */
public final class TableEditDistance implements EditDistance {

    private final EditCounters counters = new EditCounters();

    @Override
    public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        counters.reset();
        int n = a.length();
        int m = b.length();

        int[][] d = new int[n + 1][m + 1];
        counters.cellsAllocated += (long) (n + 1) * (m + 1);

        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
            counters.subproblems++;
        }
        for (int j = 1; j <= m; j++) {
            d[0][j] = j;
            counters.subproblems++;
        }

        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            int[] prev = d[i - 1];
            int[] cur = d[i];
            for (int j = 1; j <= m; j++) {
                counters.subproblems++;
                counters.comparisons++;
                if (ca == b.charAt(j - 1)) {
                    cur[j] = prev[j - 1];
                } else {
                    cur[j] = 1 + Math.min(prev[j - 1], Math.min(prev[j], cur[j - 1]));
                }
            }
        }
        return d[n][m];
    }

    @Override
    public String name() {
        return "table";
    }

    @Override
    public EditCounters counters() {
        return counters;
    }
}
