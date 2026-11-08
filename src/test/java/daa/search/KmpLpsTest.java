package daa.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class KmpLpsTest {

    private final KmpSearch kmp = new KmpSearch();

    @Test
    @DisplayName("Reference: lps(\"AAACAAAA\")")
    void referenceAaacaaaa() {
        assertArrayEquals(new int[]{0, 1, 2, 0, 1, 2, 3, 3}, kmp.lps("AAACAAAA"));
    }

    @Test
    @DisplayName("Reference: lps(\"ABABCABAB\")")
    void referenceAbabcabab() {
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 1, 2, 3, 4}, kmp.lps("ABABCABAB"));
    }

    @Test
    @DisplayName("Extra hand-checked LPS arrays")
    void extraCases() {
        assertArrayEquals(new int[]{0}, kmp.lps("A"));
        assertArrayEquals(new int[]{0, 0, 0, 0}, kmp.lps("ABCD"));
        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, kmp.lps("AAAAA"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 3, 4, 5}, kmp.lps("ABABABA"));
        assertArrayEquals(new int[]{0, 1, 0, 1, 2, 2, 2}, kmp.lps("AABAAAA"));
        assertArrayEquals(new int[]{0, 1, 2, 3, 0}, kmp.lps("AAAAB"));
    }

    @Test
    @DisplayName("Empty pattern gives an empty LPS array")
    void emptyPattern() {
        assertArrayEquals(new int[0], kmp.lps(""));
    }
}
