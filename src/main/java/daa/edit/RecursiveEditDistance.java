package daa.edit;

import java.util.Objects;

/**
 * Version A: direct recursion on the last characters, no memory. Slow on purpose.
 *
 * <p>State:      D(i, j) = edit distance between the prefixes a[0..i) and b[0..j).
 * <br>Transition: if a[i-1] == b[j-1] then D(i, j) = D(i-1, j-1), otherwise
 *                 D(i, j) = 1 + min(D(i-1, j) delete, D(i, j-1) insert, D(i-1, j-1) replace).
 * <br>Base case:  D(0, j) = j (insert j chars), D(i, 0) = i (delete i chars). Answer: D(n, m).
 *
 * <p>Nothing is stored, so the same D(i, j) is recomputed every time the recursion reaches it.
 * With no matching characters every call branches three ways and the number of calls grows
 * roughly like 5.8^n for n = m.
 */
public final class RecursiveEditDistance implements EditDistance {

    private final EditCounters counters = new EditCounters();
    private String a;
    private String b;

    @Override
    public int distance(String a, String b) {
        this.a = Objects.requireNonNull(a, "a");
        this.b = Objects.requireNonNull(b, "b");
        counters.reset();
        try {
            return solve(a.length(), b.length(), 1);
        } finally {
            this.a = null;
            this.b = null;
        }
    }

    private int solve(int i, int j, int depth) {
        counters.subproblems++;
        if (depth > counters.maxDepth) {
            counters.maxDepth = depth;
        }
        if (i == 0) {
            return j;
        }
        if (j == 0) {
            return i;
        }
        counters.comparisons++;
        if (a.charAt(i - 1) == b.charAt(j - 1)) {
            return solve(i - 1, j - 1, depth + 1);
        }
        int delete = solve(i - 1, j, depth + 1);
        int insert = solve(i, j - 1, depth + 1);
        int replace = solve(i - 1, j - 1, depth + 1);
        return 1 + Math.min(replace, Math.min(delete, insert));
    }

    @Override
    public String name() {
        return "recursive";
    }

    @Override
    public EditCounters counters() {
        return counters;
    }
}
