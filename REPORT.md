# Assignment 4 — Dynamic Programming and String Algorithms

City Search & Spell-Check: how different are two words (Edit Distance), and where does a
pattern appear in a long text (Naive vs KMP)?

## 1. Experiment Design Card

Written **before the first run** (in the git history it is committed before
`results/results.csv` exists). Changes made after the first run are listed in 1.1.

| Field | Edit Distance (recursive, table, two rows) | String Search (Naive, KMP) |
|---|---|---|
| **Question** | Does plain recursion repeat work exponentially, and do table and two rows grow like n·m, with two rows saving memory but not time? | Does Naive fall to n·m work on an adversarial input while KMP stays at ≤ 2n comparisons on every input, and how big is the gap? |
| **Sizes (n)** | **E1** (all three): n = 3…12; recursion grows ≈ 5.8ⁿ, so 12 should still take seconds. **E2** (table, two rows): n = 500…8000, doubling (×16); 8000² cells = 256 MB still fits the heap. | **S1**: n = 125 000 … 2 000 000, doubling (×16), m = 100. **S2**: n = 1 000 000, m = 4, 16, 64, 256, 1024, to isolate the pattern length. |
| **Inputs** | **random** (two unrelated a–z words): average case. **typo** (copy with 1 + n/10 random edits): the real spell-check case and the best realistic case for recursion. **disjoint** (a–m vs n–z): nothing ever matches, so recursion always branches three ways — **worst case**. | **random** (a–z text, pattern cut from it): typical case. **worst** (Aⁿ vs A^(m−1)B): every window fails after m−1 matches — **worst case** for Naive. **allmatch** (Aⁿ vs Aᵐ): every position is an overlapping match. |
| **Metrics** | `time_ms` (median of 5 after 2 warm-ups); `subproblems` (recursive calls or filled cells: the repeated work); `comparisons`; `cells_allocated` (DP memory); `max_depth` (stack); `result` (versions must agree). | `time_ms` (same); `comparisons` (textbook cost unit); `lps_comparisons` (KMP preprocessing, ≤ 2m); `backsteps` (backward moves of the text pointer, KMP must show 0); `result` (match count). |
| **Prediction** | Recursive calls ×5–6 per +1 of n on disjoint (a straight line on a log axis); random almost the same; typo far below but noisy (depends on where the typo is). Table and two rows: exactly (n+1)(m+1) cells on every input, time ×4 per doubling, flat time per cell; two rows the same speed or slightly faster with 2(m+1) cells. Recursion stops being practical at n ≈ 12–13. | random: both ≈ 1.04·n comparisons, similar times (Naive maybe slightly faster). worst and allmatch: Naive ≈ (n−m+1)·m ≈ 100n, KMP ≈ 2n and ≈ n, so a 50–100× gap. S2: the worst-case ratio grows ≈ m/2, random stays ≈ 1. |
| **Plots** | **P1** subproblems vs n, log y: exponential vs polynomial? **P2** time vs n, log y: how large an n can recursion handle? **P3** DP time vs n: quadratic, and which version is faster? **P4** time per cell (ratio): exactly ∝ n·m? | **P5** comparisons vs n, log–log: which input blows Naive up? **P6** Naive/KMP ratio vs m, log x: how does m change the gap? |

**Reproducibility.** `InputGenerator` seeds `java.util.Random` from the base seed `20261108` and
the configuration; two separate full runs gave identical counters in all 170 configurations.

### 1.1 Changes after the first run

1. Added a **limit probe E1L** (recursive, disjoint, n = 13 and 14, 3 runs): n = 12 took only
   0.75 s, so the predicted limit could not be checked otherwise.
2. **P5** became comparisons per character (a ratio), because the plot library could not label
   the planned log–log axis readably; **P7** (search time vs n) was added.
3. `time_ms` is stored with 6 decimals instead of 4, since the DP versions at n ≤ 12 take < 1 µs.

## 2. Setup and counters

MacBook Air M2 (8 GB), OpenJDK 25, `mvn -q compile exec:java` (137 s, 856 CSV rows): one seeded
input per configuration, 2 warm-ups, 5 timed runs, plots use the median. Counters are increased
inside the algorithms. `subproblems` counts every evaluation of D(i, j), exactly what recursion
repeats, so all three versions share one scale; `comparisons` is the unit of the search bounds
(n−m+1)·m and 2n, and `backsteps` checks that KMP never moves back in the text.

## 3. Plots and results

<table>
<tr>
<td width="50%"><img src="results/plots/p1_edit_subproblems_small_n.png" alt="P1"></td>
<td width="50%"><img src="results/plots/p2_edit_time_small_n.png" alt="P2"></td>
</tr>
<tr>
<td><b>P1 (log y).</b> Recursive calls grow ≈ 5.6× per character on the worst input
(11.9 billion at n = 14) while the table always fills (n+1)(m+1) cells.</td>
<td><b>P2 (log y).</b> Recursive time follows its call count (0.75 s at n = 12, 23.5 s at
n = 14), while both DP versions stay below 1 µs.</td>
</tr>
<tr>
<td><img src="results/plots/p3_edit_dp_time_large_n.png" alt="P3"></td>
<td><img src="results/plots/p4_edit_dp_ns_per_cell.png" alt="P4"></td>
</tr>
<tr>
<td><b>P3.</b> Both DP versions grow ≈ 4× per doubling of n on every input, and two rows is
≈ 1.5× faster than the table at n = 8000 (139 vs 203 ms).</td>
<td><b>P4 (ratio).</b> Two rows costs a flat 2.2–2.6 ns per cell, so its time is exactly
∝ n·m, while the table pays more per cell and is noisier.</td>
</tr>
<tr>
<td><img src="results/plots/p5_search_comparisons_per_char.png" alt="P5"></td>
<td><img src="results/plots/p6_search_ratio_vs_m.png" alt="P6"></td>
</tr>
<tr>
<td><b>P5 (ratio, log y).</b> All lines are flat (linear in n), but Naive needs 100
comparisons per character on the A…A inputs, KMP only 1–2.</td>
<td><b>P6 (ratio, log–log).</b> On the worst input the Naive/KMP gap grows ≈ m/2 (up to
≈ 510× at m = 1024), while on random text it stays ≈ 1.</td>
</tr>
<tr>
<td><img src="results/plots/p7_search_time_vs_n.png" alt="P7"></td>
<td><b>P7 (log y).</b> Times confirm the counters: Naive on the A…A inputs is ≈ 50× slower
than KMP, and KMP on allmatch is slowed down by storing ≈ 2 million matches.</td>
</tr>
</table>

## 4. Prediction vs result

**Edit Distance.** *As predicted*, disjoint calls grew ×5.1 per character at n = 4 and ×5.62 at
n = 14 (the limit is 3 + 2√2 ≈ 5.83), random words had at most 18 % fewer calls, and typo input
was cheap but noisy (23 calls at n = 8, 7.5 M at n = 12): recursion costs one call per matching
character until it meets the typo, so a typo near the end is expensive. Both DP versions filled
exactly (n+1)(m+1) cells and grew ×3.99 per doubling. *Surprise 1:* two rows was 1.1–2× faster,
not "about the same", with identical counters. The table is n+1 Java arrays, up to 256 MB to
allocate and zero, and each new row is cold in the cache, while two rows (2 × 32 KB) stay in it;
the work counters miss this memory traffic, but `cells_allocated` shows it. *Surprise 2:*
disjoint input cost ≈ 20 % more per cell (2.6 vs 2.2 ns) with the same counters; I did not
profile it, but every cell then waits for its left neighbour through `1 + min(...)`, while
matching cells on random input break that chain. The recursion limit was 13–14, not 12–13.

**String search.** *As predicted*, on random text both made 1.04 comparisons per character and
Naive was ≈ 10 % faster. On the worst input the counts were exactly (n−m+1)·m and 2n−m+1
(ratio 50.0, time 49–51×), in S2 the ratio was 2, 8, 32, 128, 511 ≈ m/2, and KMP's `backsteps`
was 0 in every run. *Surprise:* on allmatch the comparison ratio was 100 but the time ratio only
14–29×, because both algorithms must store ≈ 2 million positions as `Integer` objects. This
output cost dominates KMP's work and made those times the noisiest (up to 13× between runs),
most likely through garbage collection.

## 5. Short answers

**Which input made your string search slowest, and why?** The A…A text with the pattern A…AB
(and equally A…A with A…A): at n = 2 000 000 and m = 100 Naive needed 199 990 100 comparisons
and 390 ms, against 2.08 M and 4.3 ms on random text. Every window matches m−1 characters before
it fails, so Naive does all m comparisons, shifts by one and re-reads m−1 of them (one
`backstep` per window). Its cost is therefore (n−m+1)·m, and at m = 1024 Naive was 506× slower
than KMP. KMP stayed at 2n−m+1 comparisons; its own slowest time was on allmatch, where storing
2 million results costs more than the search.

**How large an n could the recursive Edit Distance handle, and what stopped it?** On the worst
input it handled n = 12 in 0.75 s, n = 13 in 4.2 s and n = 14 in 23.5 s (11.9 billion calls).
Each extra character multiplies the work by ≈ 5.6, so n = 15 would take about 2 minutes and
n = 20 several days. Time stopped it, not memory: the recursion is only n+m = 28 frames deep.
At n = 14 there are only 225 different subproblems, but each is recomputed ≈ 53 million times
because nothing is stored. Even "Abylai Khan" (11 characters) costs ≈ 0.1 s this way, while the
table needs under a microsecond.

**What do you gain and what do you lose when you use two rows instead of the full table?** You
gain memory: 2(m+1) instead of (n+1)(m+1) ints, i.e. 64 KB instead of 256 MB at n = 8000, so
much longer strings fit. In my runs it was also 1.1–2× faster thanks to the cache, while the
number of computed cells stays exactly n·m. You lose the table, so you can no longer trace back
which insertions, deletions and replacements produce the distance. A "Did you mean …?" feature
that highlights the changed letters would need the full table or Hirschberg's algorithm.

**If you had to choose one string search for your city platform, which one and why?** KMP. On
normal text it is as fast as Naive (within ≈ 10 %), but its cost is guaranteed: at most 2n + 2m
comparisons for any input. Log files are full of repetition, exactly where Naive slowed down 50×
(m = 100) to 500× (m = 1024), and a user-typed pattern could trigger this on purpose. KMP also
never moves back in the text, so it can scan a huge log as a stream, and its small cost on random
text is cheap insurance.

## 6. One paragraph on time

The times were stable (median slowest/fastest spread 1.03× over the 5 runs) and followed the
counters: on the worst search input the time ratios 2.0, 8.0, 31.6, 126.7, 506.4 almost equal
the comparison ratios 2.0, 8.0, 32.0, 128.0, 511.2. They are still not my only evidence. They
depend on this laptop, the JDK and the JIT, while the counters are the same on any machine for
the same seed. The clock also measures things outside the algorithm: garbage collection (KMP on
allmatch varied up to 13×), allocation (the table zeroes 256 MB) and the cache (two rows beat the
table with the same cell count), and for the DP versions at n ≤ 12 (0.2–0.8 µs) it is close to its
own resolution. So the counters show how much work is done, and the times show whether that work
really turns into seconds.
