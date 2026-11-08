package daa.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringSearchTest {

    static Stream<StringSearch> algorithms() {
        return Stream.of(new NaiveSearch(), new KmpSearch());
    }

    static Stream<Arguments> referenceMatches() {
        return algorithms().flatMap(s -> Stream.of(
                Arguments.of(s, "ABABDABACDABABCABAB", "ABABCABAB", List.of(10)),
                Arguments.of(s, "AAAAA", "AA", List.of(0, 1, 2, 3)),
                Arguments.of(s, "ABABABA", "ABA", List.of(0, 2, 4))));
    }

    @ParameterizedTest(name = "{0}: search({1}, {2}) = {3}")
    @MethodSource("referenceMatches")
    @DisplayName("Reference matches from section 7 (incl. overlapping)")
    void referenceMatches(StringSearch s, String text, String pattern, List<Integer> expected) {
        assertEquals(expected, s.search(text, pattern));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algorithms")
    @DisplayName("More overlap and boundary cases")
    void overlapAndBoundaries(StringSearch s) {
        assertEquals(List.of(0, 1, 2), s.search("AAAA", "AA"));
        assertEquals(List.of(0, 3, 6), s.search("abcabcabc", "abc"));
        assertEquals(List.of(0), s.search("same", "same"));
        assertEquals(List.of(5), s.search("xxxxxEND", "END"));
        assertEquals(List.of(0, 1, 2, 3, 4), s.search("zzzzz", "z"));
        assertEquals(List.of(), s.search("abcdef", "xyz"));
        assertEquals(List.of(17), s.search("Abylai Han, near Abylai Khan", "Abylai Khan"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algorithms")
    @DisplayName("Empty pattern throws IllegalArgumentException")
    void emptyPatternThrows(StringSearch s) {
        assertThrows(IllegalArgumentException.class, () -> s.search("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> s.search("", ""));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algorithms")
    @DisplayName("Pattern longer than text returns an empty list")
    void patternLongerThanText(StringSearch s) {
        assertEquals(List.of(), s.search("ab", "abc"));
        assertEquals(List.of(), s.search("", "a"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("algorithms")
    @DisplayName("Null arguments are rejected")
    void nullArguments(StringSearch s) {
        assertThrows(NullPointerException.class, () -> s.search(null, "a"));
        assertThrows(NullPointerException.class, () -> s.search("a", null));
    }

    @Test
    @DisplayName("100 random texts: Naive and KMP return the same list")
    void naiveAndKmpAgreeOnRandomTexts() {
        Random rnd = new Random(2026);
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        for (int t = 0; t < 100; t++) {
            // a small alphabet makes matches and partial matches frequent
            String alphabet = t % 2 == 0 ? "ab" : "abc";
            String text = randomString(rnd, rnd.nextInt(61), alphabet);
            String pattern = randomString(rnd, 1 + rnd.nextInt(6), alphabet);
            List<Integer> expected = naive.search(text, pattern);
            assertEquals(expected, kmp.search(text, pattern),
                    () -> "text=\"" + text + "\", pattern=\"" + pattern + "\"");
        }
    }

    @Test
    @DisplayName("Counters: KMP never moves back in the text, Naive does")
    void backstepCounters() {
        String text = "A".repeat(50);
        String pattern = "AAAAB";
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        naive.search(text, pattern);
        kmp.search(text, pattern);

        assertEquals(0, kmp.counters().backsteps);
        assertTrue(naive.counters().backsteps > 0);
        // Naive worst case: every one of the n-m+1 windows compares all m characters
        assertEquals((50L - 5 + 1) * 5, naive.counters().comparisons);
        // KMP bound: at most 2n comparisons in the search and 2m in the LPS construction
        assertTrue(kmp.counters().comparisons <= 2L * 50);
        assertTrue(kmp.counters().lpsComparisons <= 2L * 5);
        assertEquals(0, naive.counters().lpsComparisons);
    }

    private static String randomString(Random rnd, int len, String alphabet) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
