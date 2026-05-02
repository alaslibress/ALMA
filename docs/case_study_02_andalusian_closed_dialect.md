# Case Study 02 — Robustness on a Closed Andalusian Dialect

> Source: live `./gradlew run` invocation on 2026-04-30. Manual operator
> "fire test" outside the curated benchmark. Captured here verbatim
> because it exercises every layer of the pipeline at once and exposes
> an honest limitation in our language detector.

## Input

```
Translate to English (colloquial/rural tone): Nene Manue, ¿como le va
a tu hijo en la aceituna? que me enterao que con la lluvia eta la cosa
va pa tra.
```

The input is closed Andalusian Spanish (Sevillian rural register), not
the kind of text any general-purpose Spanish corpus contains. Salient
non-standard features:

- `Nene Manue` — vocative, dropped final `-l` of *Manuel*.
- `me enterao` — dropped intervocalic `-d-` of *enterado*.
- `lluvia eta` — dropped final `-s-` of *esta*.
- `va pa tra` — heavy contraction of *va para atrás* with elided `-s`.
- `en la aceituna` — semantic shorthand for *en la cosecha de la aceituna*
  (the olive harvest), not the fruit itself.

This is the kind of input that breaks rule-based translators outright
and reveals whether a pipeline is robust or brittle.

## Pipeline trace (verbatim)

```
[1/7] Intake          ok    [ AST -> ?? ]
[2/7] Probe           ok
[3/7] Blueprint       ok
        blueprint: Colloquial Translation Task
        tools:     [detectLanguage, tokenize, sentenceSplit,
                    normalizeUnicode, lookupTermBase]
[4/7] Mutation        ok
[5/7] SafeguardLoop   ok    (rounds: 2)
[6/7] Execution       ok
[7/7] Report          ok
```

## Output

```
Hey Manue, how's your boy doing with the olive picking? I heard with
all this rain, things are going backwards.
```

## Why this is interesting

**The Probe phase did its job.** Looking at the input, it picked
"colloquial translation" as the sub-skill — not literal translation,
not transcreation. The Blueprint title `"Colloquial Translation Task"`
reflects that. Without the probe step a generic "Spanish to English
translator" would over-formalise the output ("Hello Manuel, how is your
son doing in the olive harvest?") and lose the register that makes the
sentence what it is.

**The SafeguardLoop ran 2 rounds.** A naive single-call baseline lands
on something defensible but flat. The loop's refinement nudged the
output toward the colloquial English target.

**Every dialectal feature survived.**

| Source                | Standard ES        | ALMA EN              |
|-----------------------|--------------------|----------------------|
| `Nene Manue`          | `Pequeño Manuel`   | `Hey Manue`          |
| `como le va a tu hijo`| same               | `how's your boy`     |
| `en la aceituna`      | `en la cosecha`    | `with the olive picking` |
| `me enterao`          | `me he enterado`   | `I heard`            |
| `lluvia eta`          | `lluvia esta`      | `all this rain`      |
| `va pa tra`           | `va para atrás`    | `going backwards`    |

The semantic inference on `en la aceituna -> "olive picking"` is the
single hardest call here; it requires knowing that Andalusian rural
speech uses the crop name as shorthand for the work-in-the-fields. ALMA
got it right.

## What this reveals about the pipeline

**Stem-agent thesis confirmed in the wild.** No code change, no new
class, no new prompt template. The same `SpecializedAgent` that
translated "I will arrive late" word-for-word produced a register-
preserving rendering of closed-dialect Andalusian. The Blueprint did
all the specialization at runtime.

**Honest limitation: the language detector tagged the input as `AST`
(Asturian), not `ES`.** The Optimaize / OpenNLP detector is trained on
relatively formal text; closed Andalusian's vowel elisions and dropped
finals push the n-gram profile toward Asturian (which has similar
features). The downstream LLM did not care — `gpt-4o` handles
non-standard Spanish natively — but the language badge in the CLI
displayed `[ AST -> ?? ]`. We do not see this as a bug worth fixing
inside ALMA: the detector is doing what its training distribution
allows. We *do* note it as a future-work item: a confidence threshold
on the detector that falls back to `??` when the top language wins by
less than X probability would be more honest than asserting Asturian.

## Why this matters for the brief

The benchmark we ship is too easy on 9 of 15 entries (see
`docs/writeup.md` §3.4, "ceiling effect"). This out-of-band test does
the opposite: it stresses the pipeline end-to-end with input no one in
the team designed for, and it reveals (a) the architecture holds up,
(b) the probe + blueprint composition is what does the work, and
(c) the only weakness is in a deterministic component, not in the
adaptive one.
