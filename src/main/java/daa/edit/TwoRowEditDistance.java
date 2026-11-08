package daa.edit;

import java.util.Objects;

/**
 * Version C: the same recurrence as the table version, but only two rows are kept.
 * Time O(n*m), extra memory O(m) instead of O(n*m).
 *
 * <p>State:      cur[j] = D(i, j) for the current row i, prev[j] = D(i-1, j) for the row above.
 * <br>Transition: cur[j] = prev[j-1] if a[i-1] == b[j-1], otherwise
 *                 1 + min(prev[j], cur[j-1], prev[j-1]); then swap prev and cur.
 * <br>Base case:  prev[j] = j for row 0, and cur[0] = i at the start of row i. Answer: prev[m].
 *
 * <p>Row i only reads row i-1, so older rows can be thrown away. The price is that the full
 * table is gone, so the sequence of edits can no longer be traced back from it.
 */
public final class TwoRowEditDistance implements EditDistance {

    private final EditCounters counters = new EditCounters();

    @Override
    public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        counters.reset();
        int n = a.length();
        int m = b.length();

        int[] prev = new int[m + 1];
        int[] cur = new int[m + 1];
        counters.cellsAllocated += 2L * (m + 1);

        for (int j = 0; j <= m; j++) {
            prev[j] = j;
            counters.subproblems++;
        }

        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            counters.subproblems++;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                counters.subproblems++;
                counters.comparisons++;
                if (ca == b.charAt(j - 1)) {
                    cur[j] = prev[j - 1];
                } else {
                    cur[j] = 1 + Math.min(prev[j - 1], Math.min(prev[j], cur[j - 1]));
                }
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[m];
    }

    @Override
    public String name() {
        return "tworows";
    }

    @Override
    public EditCounters counters() {
        return counters;
    }
}
