package daa.edit;

/**
 * Work counters for one call of {@link EditDistance#distance(String, String)}.
 * Every field is increased inside the algorithm while it runs, never computed from a formula.
 *
 * <ul>
 *   <li>{@code subproblems} - how many times a value D(i, j) was produced: one recursive call
 *       for version A, one filled cell for versions B and C. This is the main cost counter:
 *       it shows directly that plain recursion solves the same subproblem many times.</li>
 *   <li>{@code comparisons} - character comparisons {@code a[i-1] == b[j-1]}.</li>
 *   <li>{@code cellsAllocated} - number of int cells the version allocated for its DP storage
 *       (the memory cost that the two-row version is supposed to cut).</li>
 *   <li>{@code maxDepth} - deepest recursion level reached (version A only, 0 otherwise).</li>
 * </ul>
 */
public final class EditCounters {
    public long subproblems;
    public long comparisons;
    public long cellsAllocated;
    public int maxDepth;

    public void reset() {
        subproblems = 0;
        comparisons = 0;
        cellsAllocated = 0;
        maxDepth = 0;
    }

    @Override
    public String toString() {
        return "subproblems=" + subproblems + ", comparisons=" + comparisons
                + ", cellsAllocated=" + cellsAllocated + ", maxDepth=" + maxDepth;
    }
}
