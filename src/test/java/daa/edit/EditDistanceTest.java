package daa.edit;

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

class EditDistanceTest {

    private static final List<EditDistance> ALL = List.of(
            new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());

    static Stream<EditDistance> versions() {
        return Stream.of(new RecursiveEditDistance(), new TableEditDistance(), new TwoRowEditDistance());
    }

    static Stream<Arguments> referenceValues() {
        return versions().flatMap(v -> Stream.of(
                Arguments.of(v, "kitten", "sitting", 3),
                Arguments.of(v, "flaw", "lawn", 2),
                Arguments.of(v, "intention", "execution", 5),
                Arguments.of(v, "", "abc", 3),
                Arguments.of(v, "abc", "abc", 0)));
    }

    @ParameterizedTest(name = "{0}: distance({1}, {2}) = {3}")
    @MethodSource("referenceValues")
    @DisplayName("Reference values from section 7")
    void referenceValues(EditDistance v, String a, String b, int expected) {
        assertEquals(expected, v.distance(a, b));
        assertEquals(expected, v.distance(b, a), "edit distance is symmetric");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    @DisplayName("Empty strings")
    void emptyStrings(EditDistance v) {
        assertEquals(0, v.distance("", ""));
        assertEquals(3, v.distance("abc", ""));
        assertEquals(1, v.distance("", "x"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    @DisplayName("Small hand-checked cases")
    void smallCases(EditDistance v) {
        // "H" -> "K" (replace) plus inserting "h": the capital H makes it 2, not 1
        assertEquals(2, v.distance("Abylai Han", "Abylai Khan"));
        assertEquals(1, v.distance("Abylai han", "Abylai khan"));
        assertEquals(1, v.distance("a", "b"));
        assertEquals(3, v.distance("abc", "xyz"));
        assertEquals(2, v.distance("ab", "ba"));
        assertEquals(8, v.distance("aaaa", "bbbbbbbb"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    @DisplayName("Null input is rejected")
    void nullInput(EditDistance v) {
        assertThrows(NullPointerException.class, () -> v.distance(null, "a"));
        assertThrows(NullPointerException.class, () -> v.distance("a", null));
    }

    @Test
    @DisplayName("100 random short strings: all three versions agree")
    void allVersionsAgreeOnRandomStrings() {
        Random rnd = new Random(2026);
        for (int t = 0; t < 100; t++) {
            String a = randomString(rnd, rnd.nextInt(9), "abc");
            String b = randomString(rnd, rnd.nextInt(9), "abc");
            int expected = ALL.get(1).distance(a, b);
            for (EditDistance v : ALL) {
                assertEquals(expected, v.distance(a, b),
                        () -> v.name() + " disagrees on (\"" + a + "\", \"" + b + "\")");
            }
        }
    }

    @Test
    @DisplayName("Counters: table fills (n+1)(m+1) cells, two rows allocate 2(m+1)")
    void countersMatchTheory() {
        String a = "intention";
        String b = "execution";
        TableEditDistance table = new TableEditDistance();
        TwoRowEditDistance rows = new TwoRowEditDistance();
        table.distance(a, b);
        rows.distance(a, b);
        assertEquals(10L * 10, table.counters().subproblems);
        assertEquals(10L * 10, rows.counters().subproblems);
        assertEquals(9L * 9, table.counters().comparisons);
        assertEquals(10L * 10, table.counters().cellsAllocated);
        assertEquals(2L * 10, rows.counters().cellsAllocated);
    }

    @Test
    @DisplayName("Counters: recursion repeats work, identical strings need only n+1 calls")
    void recursionCounters() {
        RecursiveEditDistance rec = new RecursiveEditDistance();
        rec.distance("abcdef", "abcdef");
        assertEquals(7, rec.counters().subproblems);
        rec.distance("aa", "bb");
        // C(2,2) = 1 + C(1,2) + C(2,1) + C(1,1) = 1 + 7 + 7 + 4
        assertEquals(19, rec.counters().subproblems);
        assertEquals(4, rec.counters().maxDepth, "deepest path is n+m calls long");
    }

    private static String randomString(Random rnd, int len, String alphabet) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
