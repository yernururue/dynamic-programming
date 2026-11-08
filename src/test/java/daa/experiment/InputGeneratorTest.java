package daa.experiment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputGeneratorTest {

    @Test
    @DisplayName("Same seed gives the same inputs (reproducible)")
    void sameSeedSameInput() {
        long seed = InputGenerator.seedFor("E1", "random", 10, 0);
        assertEquals(new InputGenerator(seed).editPair("random", 10),
                new InputGenerator(seed).editPair("random", 10));
        long seed2 = InputGenerator.seedFor("S1", "random", 1000, 10);
        assertEquals(new InputGenerator(seed2).searchCase("random", 1000, 10),
                new InputGenerator(seed2).searchCase("random", 1000, 10));
        assertNotEquals(seed, seed2);
    }

    @Test
    @DisplayName("Disjoint strings share no character")
    void disjointHasNoCommonCharacter() {
        InputGenerator.Pair p = new InputGenerator(1).editPair("disjoint", 200);
        for (char c : p.a().toCharArray()) {
            assertTrue(p.b().indexOf(c) < 0);
        }
    }

    @Test
    @DisplayName("Typo strings stay close to the original")
    void typoIsClose() {
        InputGenerator.Pair p = new InputGenerator(7).editPair("typo", 100);
        assertTrue(Math.abs(p.a().length() - p.b().length()) <= 1 + 100 / 10);
        assertNotEquals(p.a(), p.b());
    }

    @Test
    @DisplayName("Search inputs have the promised shape")
    void searchShapes() {
        InputGenerator g = new InputGenerator(3);
        InputGenerator.SearchCase worst = g.searchCase("worst", 50, 5);
        assertEquals("A".repeat(50), worst.text());
        assertEquals("AAAAB", worst.pattern());
        InputGenerator.SearchCase random = g.searchCase("random", 500, 20);
        assertTrue(random.text().contains(random.pattern()));
        assertEquals(20, random.pattern().length());
    }
}
