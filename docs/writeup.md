# ALMA — Adaptive Linguistic Metamorphosis Agent

JetBrains "stem agent" technical-test submission.
*All code, comments, commits, and logs are in English. The running development log is `docs/DEV_LOG.txt`. The single Spanish file is the operator quick-start `docs/EJECUCION_ES.md`.*

---

## 1. Problem framing

The brief asks for an AI agent that, like a stem cell, starts unspecialised: it reads its environment, designs itself (architecture, tools, prompts), validates itself in a safeguarded loop, and only then executes. **No hardcoded specialisations.**

We chose **linguistic transformation between English, Spanish, and Portuguese** — translation, transcreation, single-word lookup, simple style transfer. The reasoning:

- Well-defined sub-skills (source detection, terminology, register, locale, brand voice).
- Cheap, repeatable evaluation: chrF for translation, LLM-as-judge for transcreation/style.
- Strong before/after signal: a generic prompt is demonstrably worse than one with terminology and locale guidance.
- JetBrains-relevant: i18n / l10n is real concern in IDEs and dev tooling.

The agent receives a single task and must figure out the sub-type itself. It does not know in advance whether the input is translation, transcreation, localisation, or style transfer.

---

## 2. Architecture

Java 21 / Gradle (Kotlin DSL), LangChain4j on top of the OpenAI API, picocli for the CLI, OpenNLP + Optimaize + ICU4J for deterministic NLP. The architectural authority is `skill.md`.

### 2.1 The seven-phase pipeline

```
[Intake] -> [Probe] -> [Blueprint] -> [Mutation] -> [SafeguardLoop] -> [Execution] -> [Report]
```

- **Probe** (`gpt-4o-mini`) researches the class: sub-skills, tools, eval criteria, plus structured `{input, expectedReference}` synthetic test seeds the loop can score itself against.
- **Blueprint** (`gpt-4o-mini`) emits a strict-JSON `Blueprint`: title, system prompt, tool names, eval criteria, model name, score threshold, max refinement rounds.
- **Mutation** instantiates **the** single `SpecializedAgent` Java class, parameterised entirely by the Blueprint. Tool resolution is by name through `DefaultToolRegistry`. **There is exactly one `SpecializedAgent` class.** Adding a "kind of agent" requires zero new Java code; it requires a different Blueprint.
- **SafeguardLoop** scores the candidate against the synthetic tests and refines until one of four explicit stopping rules fires (§2.2).
- **Execution** runs the agent on the user's real input.
- **Report** prints the answer plus a short evolution trace.

### 2.2 Stopping rules and atomic promotion

The loop stops when **any** of these holds:

1. `score >= blueprint.scoreThreshold`.
2. `round >= blueprint.maxRefinementRounds` (default 3).
3. Two consecutive rounds without strict improvement.
4. A `MutationException` during candidate construction or scoring — atomic rollback to `lastGoodBlueprint` + `lastGoodAgent` via `AgentContext.rollback()`.

Promotion is atomic: a candidate replaces the current pair **only** through `AgentContext.promote(new, demoted)`, which sets all four state fields under one method. The "strict improvement" counter is separate; it only resets on `>`. Tie-break: equal score still promotes (newer wins) but plateau still counts. Without that split a constant-scoring agent would promote every round and never trigger rule 3 — a real bug caught by unit tests.

### 2.3 Generator / Evaluator split

Inside the loop, the **Generator** uses `Blueprint.modelName` (default `gpt-4o`, the same model used in final Execution — otherwise the loop would optimise an artefact that does not ship). The **Evaluator** judge uses `gpt-4o-mini` to bound cost. The judge prompt requires reasoning *before* the numeric scores: same-family judges otherwise reward what they would have produced themselves.

### 2.4 Defence in depth on prompts

Every JSON-bound LLM response goes through `JsonExtractor.parseOrThrow(raw, mapper, type)`, which strips outer fences, slices `{ ... }`, and throws `JsonProcessingException` (narrowed from `Exception` so `IOException` and `OutOfMemoryError` propagate). One repair retry on parse failure, then `MutationException`. Records bind without `@JsonProperty` because we ship `jackson-module-parameter-names` and compile with `-parameters`. Every classpath text resource is read with `StandardCharsets.UTF_8` to avoid Cp1252 corruption of `¡ ¿ ã ñ ç`.

---

## 3. Experiments

### 3.1 Eval harness

`./gradlew evaluate` loads `src/main/resources/benchmarks/linguistic.json` (15 hand-curated entries spanning EN↔ES, EN↔PT, ES↔PT and four sub-types), runs both the **BaselineAgent** (single LLM call, no tools, no probe) and **ALMA** (full 7-phase pipeline including the SafeguardLoop), picks chrF or LLM-as-judge per entry, prints a delta table, and writes `build/reports/eval-<timestamp>.md` with per-entry detail (status, output, trace). Per-call token usage is appended to `build/reports/token-usage.csv`.

### 3.2 Results (live run, 2026-04-30, source `eval-20260430-002631.md`)

15 entries, 7m57s, all `runStatus = OK` — no silent failures.

| Sub-type      |  N | Baseline (mean) | ALMA (mean) | Δ        |
|---------------|----|-----------------|-------------|----------|
| translation   | 10 | 0.876           | 0.854       | -0.023   |
| transcreation |  4 | 0.919           | 1.000       | +0.081   |
| style         |  1 | 1.000           | 0.900       | -0.100   |
| **overall**   | 15 | 0.896           | 0.896       | -0.000   |

The headline is honest. Reading per-entry the picture is more interesting than the mean.

**Wins.** `tc-en-pt-1` (PT-PT coffee tagline) +0.325: the refiner replaces the literal `Acorde` with `Desperte`, the register marketing actually uses (`docs/case_study_01_refinement_fixes_hallucination.md`). `tr-pt-en-1` +0.172: `walking along` over `walking on` matches the chrF reference exactly.

**SafeguardLoop rejecting a worse candidate.** A re-run on the same benchmark caught the rejection path firing on `tc-en-es-1`: trace `round 0: 0.604 initial → round 1: 0.607 promoted → round 2: 0.348 rejected → round 3: 0.607 tied`, stop at 2 rounds without improvement (`docs/case_study_03_safeguard_rejects_regression.md`). The same re-run also exercised the rollback path on a real OpenAI server error during `ex-pt-1`'s loop, recovering to the round-0 Blueprint with no stack-trace dump.

**Ceiling effect.** 9 of 15 entries land at 1.000 on both sides — the benchmark is too easy for the comparison we want.

**Out-of-band fire test.** A manual run on closed Andalusian Spanish — *"Nene Manue, ¿como le va a tu hijo en la aceituna? que me enterao que con la lluvia eta la cosa va pa tra."* — returned *"Hey Manue, how's your boy doing with the olive picking? I heard with all this rain, things are going backwards."* in 2 safeguard rounds. Probe picked `Colloquial Translation Task`; every dialectal feature (dropped finals, vowel elisions, agricultural shorthand) survived. The language detector tagged the source as `AST` (Asturian), which is a real limitation of n-gram ID on heavily elided dialects; the downstream LLM did not care. Documented in `docs/case_study_02_andalusian_closed_dialect.md`.

### 3.3 Failure analysis (honest)

For every entry where Δ < 0 OR alma < 0.4:

- **`wd-en-es-1`** *"What is 'deadline' in Spanish?"* — baseline `Fecha límite.` (0.872) vs ALMA `The term 'deadline' in Spanish is translated as "fecha límite."` (0.481). The Blueprint's prompt does not constrain output length; ALMA's English explanation wrapper kills chrF. The fix is at the prompt template, not the architecture.
- **`wd-en-pt-1`** *"What is 'branch' in Portuguese (software)?"* — baseline 0.125, ALMA 0.119 (Δ -0.006). Same verbose-wrapper root cause plus a chrF artefact: 1–2-word references make the metric extremely sensitive to extra characters.
- **`st-en-1`** *"Rewrite this sentence in a formal register..."* — judge-scored. Baseline and ALMA produced **byte-identical output**, yet the judge gave them 1.000 vs 0.900. The 0.100 gap exists with the same string. The single-row judge is not stable enough; future work averages over re-runs.
- **`tr-en-pt-2`** — both produced grammatically valid PT-BR (`melhore nossas ferramentas`); reference asked for `melhore as nossas ferramentas`. Reference too narrow.

**Failure modes.** (1) Verbosity costs chrF on single-term lookups. (2) Single-row judge non-determinism dominates 0.1 deltas. (3) Ceiling effect: 9/15 entries too easy. (4) Reference brittleness: chrF rewards exact matches; equally valid grammatical variants lose score.

The brief explicitly says a careful failure analysis beats a fake 100%. We agree, and we kept the table honest.

---

## 4. Reflections

### 4.1 What surprised us

- **Markdown fences are the LLM's default JSON output.** The 35-line `JsonExtractor` is the single most useful file in the project; without it the pipeline breaks on every other call.
- **Apache OpenNLP's legacy 1.5 server has only English models.** The right answer was the UD-1.3 model server, which has EN/ES/PT — total disk footprint 54 KB.
- **chrF was less scary than its reputation.** The plan reserved a 1.5h time-box; the implementation took ~30 minutes and ~70 lines, confirmed against a hand-computed `"abc"` vs `"abd"` → 7/18 reference.
- **Promotion `>=` and patience `>` are different concepts.** With a constant scorer, naïve `>` everywhere never fires the patience rule; naïve `>=` everywhere never resets it. They have to be split. Caught only by the stopping-rule unit tests.
- **Closed-dialect robustness.** The Andalusian fire test (§3.2) showed the architecture absorbs heavily non-standard input that no team member designed for; the only weakness was the deterministic language detector, not the adaptive parts.

### 4.2 What we threw out

- **JSON-Patch refinement.** A diff-and-merge layer in pure Java fought the simplicity rule. The refiner now returns the whole updated Blueprint and overwrites atomically — fewer lines, no magic.
- **COMET as a third metric.** Forces ONNX runtime or a Python sidecar; chrF + LLM-as-judge cover translation and style cleanly and stay JVM-native.
- **Per-call NLP model loading.** A 2 MB binary read per `sentenceSplit` would have killed latency; `DefaultToolRegistry` holds eager singletons.

### 4.3 What we would do with more time

- Tighten the Blueprint prompt so single-term lookups emit only the term (the largest regression measured, `wd-en-es-1`).
- Average judge scores over 3 re-runs to smooth single-row variance (the `st-en-1` 0.100 gap on byte-identical strings).
- Expand the benchmark to 30–50 entries weighted toward harder cases so the ceiling effect on 9/15 entries no longer hides the loop's contribution.
- Add a confidence threshold on the language detector that falls back to `??` when the top probability wins by a small margin (the Andalusian → Asturian misclassification).
- Persist the chosen `Blueprint` to disk for offline replay (`--save-blueprint`).
- Add `targetLanguage` as a `Blueprint` field so `LanguageBadge` can read it directly instead of relying on `--lang`.

### 4.4 Honest time accounting

01–02 (bootstrap + pipeline) ~1 day combined; 03 (LLM/Probe/Blueprint, including the JsonExtractor and the Jackson + records discovery) ~1 day; 04 (Mutation + SafeguardLoop, the patience-counter bug ate an evening) ~1 day; 05 (Tools + NLP, mostly tracking down the UD-1.3 model server) ~half day; 06 (CLI + UI + ghost) ~half day; 07 (eval harness) ~half day; 08 + 09 (write-up + hardening + live eval + case studies + README polish + Spanish quick-start) ~1.5 days.

`docs/DEV_LOG.txt` records every meaningful decision, surprise, and failure with timestamps. Read it top-to-bottom to follow the path of thinking.
