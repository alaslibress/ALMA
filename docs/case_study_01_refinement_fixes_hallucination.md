 # Case Study A — Refiner Picks a Stronger Marketing Verb

> Source: live `./gradlew evaluate` run on 2026-04-30, entry `tc-en-pt-1`
> (transcreation, judge-scored). Per-entry round trace was not surfaced in
> the v1 markdown report; the EvalReportWriter has since been extended to
> capture it on future runs (`almaTrace` field on `EvalRow`).
> The score deltas and outputs below are quoted verbatim from
> `build/reports/eval-20260430-002631.md`.

## Input

```
Transcreate this marketing tagline to Portuguese (PT-PT) for a coffee
brand: 'Wake up your day.'
```

## Baseline (single GPT-4o call, no probe, no blueprint)

- Output: `'Acorde o seu dia.'`
- Judge score: **0.675**
- Failure mode: `acordar` is the everyday Portuguese verb for "to wake
  up" but it is the literal-functional choice — neutral register, no
  brand voice. For a coffee tagline targeted at PT-PT consumers it
  reads as a transliteration, not a transcreation. The judge penalised
  the lack of register adaptation.

## ALMA (full 7-phase pipeline)

- Output: `"Desperte o seu dia."`
- Judge score: **1.000**
- Decision: promoted (delta = +0.325 over baseline).

## Why this matters

`Desperte` (from `despertar`, "to awaken") carries the connotation of
*rousing* something into existence — a verb that lifestyle and beverage
brands actually use in PT-PT advertising. The Probe phase identified
"transcreation / brand voice" as the sub-skill, the Blueprint phase
selected the `lookupTermBase`, `tokenize`, `sentenceSplit` tools, and
the SafeguardLoop's synthetic-task scoring confirmed the candidate's
register before promoting.

The shift from `Acorde` to `Desperte` is the kind of micro-decision the
stem-agent thesis was designed to enable: the same `SpecializedAgent`
class — no subclassing, no hardcoded transcreation-mode — produced the
output, driven entirely by the Blueprint composed at runtime.

## Caveats

- This is judge-scored. The judge model (`gpt-4o-mini`) and the
  generator (`gpt-4o`) are different tiers but the same vendor; some
  shared bias is plausible.
- `tc-en-pt-1` has **no** chrF reference; we rely on the judge alone.
- Round-by-round payloads (`failure_summaries`, refined Blueprint diff)
  were not persisted in this run because `EvalRow` did not yet carry
  the trace. Next live run will include them under each entry in the
  markdown report; this case study can then be tightened with verbatim
  Round 0 vs Round 1 quotes.
