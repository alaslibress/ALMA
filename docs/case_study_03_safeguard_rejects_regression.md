# Case Study 03 — SafeguardLoop Rejects a Worse Candidate

> Source: live `./gradlew evaluate` re-run on 2026-04-30 (second run,
> report `build/reports/eval-20260430-012905.md`). Per-entry evolution
> trace is now embedded in the markdown report.

This is the rejection-of-worse case study mandated by plan09 §2.4. It
demonstrates rule 3 of the SafeguardLoop's stopping criteria:
**a candidate is promoted only when its score is `>=` the current
score; a strict drop is rejected and the previous Blueprint is kept**.
The atomic `(Blueprint, Agent)` pair is preserved through
`AgentContext.promote(...)` / `AgentContext.rollback()`.

## Input

```
Transcreate this marketing tagline to Spanish (Mexico) for a
streaming service: 'Stories that move you.'
```

Entry id: `tc-en-es-1` (transcreation, judge-scored, no chrF reference).

## Verbatim trace (from the markdown report)

```
round 0: score=0,604 initial
round 1: score=0,607 promoted
round 2: score=0,348 rejected
round 3: score=0,607 tied (newer wins, plateau counted)
stop: 2 rounds without improvement at round 3
```

## Reading the trace

- **Round 0.** Initial Blueprint built from the Probe. Synthetic-task
  score on the loop's internal benchmark: 0.604. Below the threshold,
  so the loop proceeds.
- **Round 1.** Refiner returns a candidate Blueprint. Synthetic score
  rises to 0.607 — a tiny strict gain. `decide(0.604, 0.607)` returns
  `(promote=true, resetsPatience=true, tag="promoted")`. Patience
  counter resets to 0. State now points to the round-1 Blueprint and
  agent.
- **Round 2.** Refiner proposes another candidate. Synthetic score
  collapses to 0.348 — a 0.259 drop. `decide(0.607, 0.348)` returns
  `(promote=false, resetsPatience=false, tag="rejected")`. **The
  current Blueprint and Agent are not touched.** Patience counter
  ticks to 1. The round-1 Blueprint stays in `context.blueprint()`.
- **Round 3.** Refiner proposes another candidate. Score back to
  0.607 — exactly equal to the kept round-1 score. `decide` returns
  `tied (newer wins, plateau counted)`: promote on equality (the
  refiner had a reason to propose it), but the patience counter still
  ticks to 2.
- **Stop.** Patience >= 2 → rule 3 fires. Loop exits with the round-3
  Blueprint (which is equivalent to round-1 by score, but the newer
  one wins by design).

## Why this matters

The architecture has to *both* be willing to keep trying after a setback
*and* refuse to be dragged below its current performance. Naïve "keep
the higher score" works on round 2 (it does); naïve "always promote
the latest" would have shipped the 0.348 candidate. The split between
promotion (`>=`) and patience (`>`) is the cheapest possible
implementation of that intent, and the trace shows it working on a real
LLM-generated regression in production.

The final user-facing output for this row scored **0.900** on the
external judge — a different scorer than the synthetic-task scorer
that drives the loop. The ALMA output was verbose ("Transcreating the
tagline... Here's a transcreation:..."), and the baseline's terse
`'Historias que te conmueven.'` got the maximum 1.000. This is the
verbosity issue documented as failure mode 1 in `docs/writeup.md` §3.3.
The two findings are independent: the loop rejected a worse candidate
*correctly* on its internal scorer; the external judge separately
rewards terseness on this specific row.

## Bonus: rollback path on real OpenAI flake

The same eval re-run also hit OpenAI's server-side flakiness. Entry
`ex-pt-1` produced this trace:

```
round 0: score=0,404 initial
round 1: score=0,387 rejected
stop: mutation error round 2: dev.ai4j.openai4j.OpenAiHttpException:
  { "error": { "message": "The server had an error while processing
  your request. Sorry about that!", "type": "server_error", ... } }
```

Round 1 was rejected on score (`0,387 < 0,404`). Round 2 hit a real
HTTP server error from OpenAI's side. The loop caught the exception,
appended the `stop: mutation error...` trace line, called
`AgentContext.rollback()` to restore the round-0 (Blueprint, Agent)
pair, and returned. Execution then ran on that restored agent and the
final user-facing output scored **1.000** on the external judge.

This is the rollback path firing in production on a non-synthetic
upstream failure — exactly what `MutationException` plus the atomic
rollback method are there for. The user did not see a stack-trace dump;
they saw a clean answer.

## Numbers from the same re-run that prove judge variance

The transcreation aggregate moved from **+0.081** in the first run to
**-0.431** in the second run on the *same* benchmark. Same code, same
prompts, same models, same temperature. Two of the four transcreation
rows landed on `runStatus = FAILED` in the second run while none failed
in the first. Single-run aggregates on a 4-row sub-type are not stable;
single-row judge deltas like the byte-identical 0.100 gap on `st-en-1`
are even less stable. Future work averages over multiple re-runs.
