package daa.search;

import java.util.List;
import java.util.Objects;

/**
 * Finds all occurrences of a pattern P in a text T.
 *
 * <p>Contract shared by every implementation:
 * <ul>
 *   <li>returns the 0-based start indices of <b>all</b> occurrences in increasing order,
 *       overlapping ones included;</li>
 *   <li>returns an empty list if there is no match or the pattern is longer than the text;</li>
 *   <li>throws {@link IllegalArgumentException} for an empty pattern and
 *       {@link NullPointerException} for null arguments.</li>
 * </ul>
 * Every call resets {@link #counters()} first. Implementations are not thread-safe.
 */
public interface StringSearch {

    List<Integer> search(String text, String pattern);

    /** Short name used in results.csv, e.g. "naive". */
    String name();

    /** Counters of the last {@link #search(String, String)} call. */
    SearchCounters counters();

    /** Shared argument checks, so every implementation follows the same rules. */
    static void validate(String text, String pattern) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("pattern must not be empty");
        }
    }
}
