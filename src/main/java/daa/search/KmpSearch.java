package daa.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Knuth-Morris-Pratt search.
 *
 * <p>First the LPS array is built: lps[i] = length of the longest proper prefix of
 * P[0..i] that is also a suffix of P[0..i]. During the search, j is the number of pattern
 * characters matched so far. On a mismatch at P[j], KMP does not move back in the text: it knows
 * that the last j text characters equal P[0..j), so it can continue with j = lps[j-1] (the
 * longest prefix that is still matched) and compare the <i>same</i> text character again.
 * After a full match it continues with j = lps[m-1], which finds overlapping occurrences.
 *
 * <p>The text index i only grows, so the search makes at most 2n comparisons, and the LPS
 * construction at most 2m: O(n + m) in total for every input.
 */
public final class KmpSearch implements StringSearch {

    private final SearchCounters counters = new SearchCounters();

    /**
     * Builds the prefix function (LPS array) of {@code p}.
     * Comparisons are added to {@link SearchCounters#lpsComparisons} (not reset here).
     *
     * @return array where lps[i] is the length of the longest proper prefix of p[0..i]
     *         that is also a suffix of p[0..i]; an empty array for an empty pattern
     */
    public int[] lps(String p) {
        Objects.requireNonNull(p, "p");
        int m = p.length();
        int[] lps = new int[m];
        int len = 0; // length of the current longest prefix-suffix
        int i = 1;
        while (i < m) {
            counters.lpsComparisons++;
            if (p.charAt(i) == p.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1]; // fall back to a shorter prefix-suffix, i stays
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    @Override
    public List<Integer> search(String text, String pattern) {
        StringSearch.validate(text, pattern);
        counters.reset();
        List<Integer> result = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();
        if (m > n) {
            return result;
        }

        int[] lps = lps(pattern);
        int j = 0;
        int lastRead = -1;
        for (int i = 0; i < n; i++) { // i never decreases
            char c = text.charAt(i);
            while (true) {
                counters.comparisons++;
                if (i < lastRead) {
                    counters.backsteps++;
                }
                lastRead = i;
                if (c == pattern.charAt(j)) {
                    j++;
                    if (j == m) {
                        result.add(i - m + 1);
                        j = lps[m - 1];
                    }
                    break;
                }
                if (j == 0) {
                    break;
                }
                j = lps[j - 1];
            }
        }
        counters.matches = result.size();
        return result;
    }

    @Override
    public String name() {
        return "kmp";
    }

    @Override
    public SearchCounters counters() {
        return counters;
    }
}
