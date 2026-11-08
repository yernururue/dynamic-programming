package daa.edit;

/**
 * Edit distance: the smallest number of insertions, deletions and replacements
 * that turn string {@code a} into string {@code b}.
 *
 * <p>All versions share this one signature, so the experiment can loop over them.
 * Every call resets {@link #counters()} first, so after a call the counters describe
 * exactly that call. Implementations are not thread-safe.
 */
public interface EditDistance {

    /** Returns the edit distance between {@code a} and {@code b}. Both must be non-null. */
    int distance(String a, String b);

    /** Short name used in results.csv, e.g. "recursive". */
    String name();

    /** Counters of the last {@link #distance(String, String)} call. */
    EditCounters counters();
}
