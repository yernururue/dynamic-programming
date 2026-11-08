# Assignment 4 — Dynamic Programming and String Algorithms

City Search & Spell-Check: how different are two words (Edit Distance), and where does a
pattern appear in a long text (Naive vs KMP)?

## 1. Experiment Design Card

Written **before the first run** (see the git history: this card is committed before
`results/results.csv` exists). Changes made after the first run are listed in 1.1.

| Field | Edit Distance (recursive, table, two rows) | String Search (Naive, KMP) |
|---|---|---|
| **Question** | Does plain recursion really repeat work exponentially, and do the table and two-row versions grow exactly like n·m, with two rows saving memory but not time? | Does Naive fall to n·m work on an adversarial input while KMP stays linear (≤ 2n comparisons) on every input, and how large is the gap in practice? |
| **Sizes (n)** | **E1** (all three versions): n = 3, 4, …, 12. Recursion grows roughly like 5.8ⁿ, so 12 is the largest size I expect to finish in a few seconds; the DP versions run on the same sizes so all three can share one plot. **E2** (table, two rows): n = 500, 1000, 2000, 4000, 8000 (×16). 8000² = 64 M cells, the table then needs 256 MB, which still fits into the default heap. Both strings have length n (typo input: about n). | **S1** (vary the text): n = 125 000, 250 000, 500 000, 1 000 000, 2 000 000 with m = 100 (×16). **S2** (vary the pattern): n = 1 000 000 with m = 4, 16, 64, 256, 1024 (×256), to see the effect of pattern length alone. |
| **Inputs** | **random** — two unrelated words over a–z: the average case. **typo** — b is a copy of a with 1 + n/10 random substitutions, insertions or deletions: the real spell-check case; similar strings are the best realistic case for recursion, because equal last characters lead to only one recursive call. **disjoint** — a uses only a–m, b only n–z: no character ever matches, so every recursive call branches three ways — the **worst case** for recursion. | **random** — a–z text, pattern cut out of the text (at least one match): typical search, most windows fail on the first character. **worst** — T = Aⁿ, P = A^(m−1)B: every window matches m−1 characters and then fails — the **worst case** for Naive. **allmatch** — T = Aⁿ, P = Aᵐ: every position is an overlapping match; it checks overlap handling under load and also costs Naive m comparisons per window. |
| **Metrics** | `time_ms`: median of 5 runs after 2 warm-up runs — the real cost on this laptop. `subproblems`: recursive calls (A) or filled cells (B, C) — machine-independent work, shows repeated work directly. `comparisons`: character comparisons. `cells_allocated`: ints of DP storage — the memory that two rows should save. `max_depth`: recursion depth (stack use). `result`: the distance, to check that the versions agree inside the experiment too. | `time_ms`: same method. `comparisons`: text-vs-pattern character comparisons — the textbook cost unit. `lps_comparisons`: KMP preprocessing cost (≤ 2m). `backsteps`: how often the text pointer moved backwards (KMP must show 0). `result`: number of matches, to check that both agree. |
| **Prediction** | Recursive `subproblems` multiply by about 5–6 per +1 of n on disjoint input — a straight rising line on a log axis. Random looks almost the same (random letters match with probability 1/26). Typo is far below, but noisy: only the misaligned branches explode, and how much depends on where the typo is. Table and two rows: exactly (n+1)(m+1) cells for every input, time ×4 per doubling of n, so time per cell is flat. Two rows: the same speed or slightly faster (two short rows stay in the cache) and 2(m+1) cells instead of (n+1)(m+1). Recursion should stop being practical at about n = 12–13. | random: both ≈ 1.04·n comparisons and nearly equal times (Naive may even be a little faster: simpler loop, no LPS). worst and allmatch: Naive ≈ (n−m+1)·m ≈ 100·n comparisons, KMP ≈ 2n (worst) and ≈ n (allmatch), so 50–100× fewer comparisons and a time gap of the same order. S2: on the worst input the Naive/KMP ratio grows linearly with m (≈ m/2); on random input it stays ≈ 1. |
| **Plots** | **P1** subproblems vs n, log y: does recursion repeat work exponentially while DP stays polynomial? **P2** time vs n, log y: how large an n can recursion handle? **P3** time vs n for table and two rows: is the growth quadratic, and which version is faster? **P4** time per cell vs n (ratio): is the cost exactly proportional to n·m? | **P5** comparisons vs n, log–log, one line per algorithm and input: which input blows Naive up? **P6** Naive/KMP ratio of comparisons and of time vs m, log x: how does the pattern length change the gap? |

**Reproducibility.** Every input is produced by `daa.experiment.InputGenerator` with
`java.util.Random` and a seed derived from the fixed base seed `20261108` and the
configuration (experiment, input, n, m). The same command gives the same inputs and the same
counters; only the times depend on the machine.

### 1.1 Changes after the first run

_None yet._
