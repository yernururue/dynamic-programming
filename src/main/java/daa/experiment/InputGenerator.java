package daa.experiment;

import java.util.Random;

/**
 * Reproducible input generator. Every input is built from {@link java.util.Random} with a seed
 * that depends only on the fixed {@link #BASE_SEED} and the configuration, so the same command
 * always produces the same strings (and therefore the same counters).
 */
public final class InputGenerator {

    public static final long BASE_SEED = 20261108L;

    /** Two strings for Edit Distance. */
    public record Pair(String a, String b) { }

    /** A text and a pattern for string search. */
    public record SearchCase(String text, String pattern) { }

    private final Random rnd;

    public InputGenerator(long seed) {
        this.rnd = new Random(seed);
    }

    /** Seed for one configuration; String.hashCode is specified by the JLS, so this is stable. */
    public static long seedFor(String experiment, String input, int n, int m) {
        long h = BASE_SEED;
        h = h * 1_000_003L + experiment.hashCode();
        h = h * 1_000_003L + input.hashCode();
        h = h * 1_000_003L + n;
        h = h * 1_000_003L + m;
        return h;
    }

    // ------------------------------------------------------------------ Edit Distance inputs

    /**
     * @param type "random"   - two unrelated words over a-z (average case);
     *             "typo"     - b = a with 1 + n/10 random substitutions/insertions/deletions
     *                          (realistic spell-check case, similar strings);
     *             "disjoint" - a over a-m, b over n-z, no character ever matches
     *                          (worst case for the recursive version).
     */
    public Pair editPair(String type, int n) {
        switch (type) {
            case "random":
                return new Pair(randomString(n, 'a', 'z'), randomString(n, 'a', 'z'));
            case "typo": {
                String a = randomString(n, 'a', 'z');
                return new Pair(a, withTypos(a, 1 + n / 10));
            }
            case "disjoint":
                return new Pair(randomString(n, 'a', 'm'), randomString(n, 'n', 'z'));
            default:
                throw new IllegalArgumentException("unknown edit input type: " + type);
        }
    }

    /** Applies {@code count} random edits (substitute / insert / delete) to {@code s}. */
    String withTypos(String s, int count) {
        StringBuilder sb = new StringBuilder(s);
        for (int k = 0; k < count; k++) {
            int op = sb.length() <= 1 ? 1 : rnd.nextInt(3);
            if (op == 0) {                          // substitute with a different letter
                int pos = rnd.nextInt(sb.length());
                char old = sb.charAt(pos);
                char c;
                do {
                    c = randomChar('a', 'z');
                } while (c == old);
                sb.setCharAt(pos, c);
            } else if (op == 1) {                   // insert
                sb.insert(rnd.nextInt(sb.length() + 1), randomChar('a', 'z'));
            } else {                                // delete
                sb.deleteCharAt(rnd.nextInt(sb.length()));
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ String search inputs

    /**
     * @param type "random"   - a-z text, pattern cut out of the text (typical case);
     *             "worst"    - T = A^n, P = A^(m-1) B: every window matches m-1 characters
     *                          and then fails (worst case for Naive);
     *             "allmatch" - T = A^n, P = A^m: every position is an overlapping match.
     */
    public SearchCase searchCase(String type, int n, int m) {
        if (m < 1 || m > n) {
            throw new IllegalArgumentException("need 1 <= m <= n, got n=" + n + ", m=" + m);
        }
        switch (type) {
            case "random": {
                String text = randomString(n, 'a', 'z');
                int pos = rnd.nextInt(n - m + 1);
                return new SearchCase(text, text.substring(pos, pos + m));
            }
            case "worst":
                return new SearchCase("A".repeat(n), "A".repeat(m - 1) + "B");
            case "allmatch":
                return new SearchCase("A".repeat(n), "A".repeat(m));
            default:
                throw new IllegalArgumentException("unknown search input type: " + type);
        }
    }

    // ------------------------------------------------------------------ helpers

    String randomString(int len, char from, char to) {
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = randomChar(from, to);
        }
        return new String(chars);
    }

    private char randomChar(char from, char to) {
        return (char) (from + rnd.nextInt(to - from + 1));
    }
}
