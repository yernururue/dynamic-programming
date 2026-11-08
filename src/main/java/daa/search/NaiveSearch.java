package daa.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Naive string search: try every start position s = 0..n-m and compare P with T[s..s+m)
 * character by character, stopping at the first mismatch.
 *
 * <p>Best case about n-m+1 comparisons (the first character mismatches almost everywhere),
 * worst case (n-m+1)*m comparisons (for example T = "AAAA...A", P = "AA...AB"), because after a
 * partial match of length k the next attempt starts again at s+1 and re-reads k-1 characters.
 */
public final class NaiveSearch implements StringSearch {

    private final SearchCounters counters = new SearchCounters();

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

        int lastRead = -1;
        for (int s = 0; s <= n - m; s++) {
            int j = 0;
            while (j < m) {
                int t = s + j;
                counters.comparisons++;
                if (t < lastRead) {
                    counters.backsteps++;
                }
                lastRead = t;
                if (text.charAt(t) != pattern.charAt(j)) {
                    break;
                }
                j++;
            }
            if (j == m) {
                result.add(s);
            }
        }
        counters.matches = result.size();
        return result;
    }

    @Override
    public String name() {
        return "naive";
    }

    @Override
    public SearchCounters counters() {
        return counters;
    }
}
