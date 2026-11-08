package daa.search;

/**
 * Work counters for one call of {@link StringSearch#search(String, String)}.
 * All fields are increased inside the algorithm while it runs.
 *
 * <ul>
 *   <li>{@code comparisons} - character comparisons between the text and the pattern
 *       (the classic cost unit for string matching: Naive can need (n-m+1)*m of them,
 *       KMP at most 2n).</li>
 *   <li>{@code lpsComparisons} - character comparisons made while building the LPS array
 *       (KMP preprocessing, at most 2m; always 0 for Naive).</li>
 *   <li>{@code backsteps} - how many times a comparison read a text position that lies
 *       before the position read by the previous comparison, i.e. the text pointer moved
 *       backwards. KMP promises 0 here; Naive moves back after every partial match.</li>
 *   <li>{@code matches} - number of occurrences reported.</li>
 * </ul>
 */
public final class SearchCounters {
    public long comparisons;
    public long lpsComparisons;
    public long backsteps;
    public long matches;

    public void reset() {
        comparisons = 0;
        lpsComparisons = 0;
        backsteps = 0;
        matches = 0;
    }

    @Override
    public String toString() {
        return "comparisons=" + comparisons + ", lpsComparisons=" + lpsComparisons
                + ", backsteps=" + backsteps + ", matches=" + matches;
    }
}
