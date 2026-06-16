# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

JThaiPDF is a small Java library that fixes Thai-language rendering in PDFs generated via iText (the legacy `com.lowagie.text` / OpenPDF API) and JasperReports. It is published as the `com.googlecode.jthaipdf:jthaipdf` JAR.

## Build & publish

- Build / package: `mvn -B package` (compiles to Java 1.8, `target/jthaipdf.jar`)
- The project targets Java 8 but the CI workflow builds on JDK 11. There are **no tests** — JUnit is declared but `src/test` does not exist, so `mvn test` is a no-op.
- Publishing is driven by Maven profiles selected with `-Drepository=...`:
  - GitHub Packages: `mvn deploy -Drepository=github` (CI does this automatically on GitHub release via `.github/workflows/maven-publish.yml`, which needs a `settings.xml` providing `GITHUB_TOKEN`).
  - Google Artifact Registry: `mvn deploy -Drepository=gar` (uses the `artifactregistry-maven-wagon` extension; requires GCP auth, e.g. `gcloud auth application-default login`).

## Architecture

The whole library exists to solve one problem: Thai text stacks combining vowels/tone marks above and below base consonants, and PDF fonts do not reposition these glyphs automatically. The fix is to remap Unicode Thai characters to **Private Use Area (PUA, 0xF7xx) glyph variants** that are pre-shifted/pulled-down so they don't collide. Everything else is integration glue.

- `util/ThaiDisplayUtils.java` — the core algorithm and the only place with real logic. `toDisplayString(...)` walks the character array, classifies each char by level (upper level 1, upper level 2, lower level) and by the tail shape of the preceding base consonant (up-tail / down-tail), then substitutes PUA variants via `shiftLeft`, `pullDown`, `pullDownAndShiftLeft`, and `cutTail`. It also explodes SARA_AM (ำ) into NIKHAHIT + SARA_AA before processing. All the `0xExx` (standard Thai) → `0xF7xx` (PUA) mappings are the `public static final char` constants at the bottom of the file. **When changing glyph behavior, edit the classification predicates and the switch-based mappers here** — the rules are positional and order-dependent, so the loop's look-back logic (`pch`, the `i-2` adjustment for lower-level chars) matters.

- `itext/ThaiChunk.java` — drop-in subclass of iText `Chunk`. Each constructor calls `manageContent()`, which runs `ThaiDisplayUtils.toDisplayString` over the inherited `content` field in place. Usage: replace `Chunk` with `ThaiChunk`, or wrap an existing `Chunk`. The "wrap a Chunk" constructor exists specifically to preserve styling (e.g. underline) that is lost if you rebuild from a raw string.

- `jasperreports/engine/export/ThaiJRPdfExporter.java` — subclass of JasperReports `JRPdfExporter` that overrides `getChunk(...)` to wrap every chunk in a `ThaiChunk`. This is how Thai correction is injected into the JasperReports PDF export pipeline.

- `jasperreports/engine/ThaiExporterManager.java` — thin convenience wrapper exposing `exportReportToPdfFile` / `exportReportToPdfStream` so callers don't have to configure `ThaiJRPdfExporter` parameters by hand.

## Conventions

- Targets the **old iText API under `com.lowagie.text`** (OpenPDF lineage), not modern iText 5/7. JasperReports is pinned to 4.0.1 with many transitive deps excluded in `pom.xml`; both are old and intentionally so.
- The PUA codepoints assume a font that ships the Thai PUA glyph set (the standard Acrobat/Adobe Thai font layout). The mapping is meaningless without such a font in the PDF.
