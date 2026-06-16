# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

JThaiPDF is a small Java library that fixes Thai-language rendering in JasperReports PDF output. As of `2.0.0-jr7` it targets **JasperReports 7.x** (`net.sf.jasperreports.pdf`); earlier `1.x` targeted JasperReports 4.0.1 and the legacy iText `com.lowagie.text` API. It is published as the `com.googlecode.jthaipdf:jthaipdf` JAR.

Note that JasperReports 7.x has a **native** fix for this exact problem — the report/context property `net.sf.jasperreports.export.pdf.glyph.renderer.blocks.x=thai` makes JR shape Thai via Java/AWT, with no PUA-font dependency. This library is the alternative for fonts that ship the legacy Thai PUA glyph set; see `README`.

## Build & publish

- Build / package: `mvn -B package` (compiles with `--release 17`, `target/jthaipdf.jar`)
- Requires JDK 17+. The compiler targets release 17 and tests use JUnit 6 (which itself requires Java 17); JasperReports 7.0.7 is actually Java 8 bytecode, so the floor comes from the toolchain, not JR. The `maven-publish.yml` CI workflow sets up JDK 17 to match.
- Tests (JUnit 6/Jupiter, via surefire):
  - `ThaiDisplayUtilsTest` — characterization tests for the `ThaiDisplayUtils` glyph algorithm.
  - `PdfRenderHarnessTest` (in `jasperreports/`) — builds a `JasperPrint` and exports two PDFs to `target/`: `thai-native.pdf` (JR's built-in glyph renderer) and `thai-library.pdf` (this library) for visual comparison across normal + edge/rare Thai cases. Its font (TH Sarabun New) is bundled and registered via `src/test/resources/jasperreports_extension.properties` (+ `fonts/`). Gotcha: a hand-built `JRPrintText` needs `setTextHeight(...)` or the PDF exporter draws nothing.
- Publishing is driven by Maven profiles selected with `-Drepository=...`:
  - GitHub Packages: `mvn deploy -Drepository=github` (CI does this automatically on GitHub release via `.github/workflows/maven-publish.yml`, which needs a `settings.xml` providing `GITHUB_TOKEN`).
  - Google Artifact Registry: `mvn deploy -Drepository=gar` (uses the `artifactregistry-maven-wagon` extension; requires GCP auth, e.g. `gcloud auth application-default login`).

## Architecture

The whole library exists to solve one problem: Thai text stacks combining vowels/tone marks above and below base consonants, and PDF fonts do not reposition these glyphs automatically. The fix is to remap Unicode Thai characters to **Private Use Area (PUA, 0xF7xx) glyph variants** that are pre-shifted/pulled-down so they don't collide. Everything else is integration glue.

- `util/ThaiDisplayUtils.java` — the core algorithm and the only place with real logic. `toDisplayString(...)` walks the character array, classifies each char by level (upper level 1, upper level 2, lower level) and by the tail shape of the preceding base consonant (up-tail / down-tail), then substitutes PUA variants via `shiftLeft`, `pullDown`, `pullDownAndShiftLeft`, and `cutTail`. It also explodes SARA_AM (ำ) into NIKHAHIT + SARA_AA before processing. All the `0xExx` (standard Thai) → `0xF7xx` (PUA) mappings are the `public static final char` constants at the bottom of the file. **When changing glyph behavior, edit the classification predicates and the switch-based mappers here** — the rules are positional and order-dependent, so the loop's look-back logic (`pch`, the `i-2` adjustment for lower-level chars) matters.

- `jasperreports/engine/export/ThaiJRPdfExporter.java` — subclass of `net.sf.jasperreports.pdf.JRPdfExporter` (JR 7.x). It overrides `protected PdfTextChunk getChunk(Map<AttributedCharacterIterator.Attribute,Object>, String text, Locale)` and rearranges `text` with `ThaiDisplayUtils.toDisplayString(text)` **before** delegating to `super.getChunk(...)`. Because the JR 7.x PDF layer is abstracted behind a producer (the chunk text arrives as a `String` and the framework builds the `PdfTextChunk`), no chunk-wrapping is needed — this is why the old iText `ThaiChunk` was removed in 2.0.0-jr7.

- `jasperreports/engine/ThaiExporterManager.java` — thin convenience wrapper (`exportReportToPdfFile` / `exportReportToPdfStream`). Uses the JR 7.x fluent API (`SimpleExporterInput` + `SimpleOutputStreamExporterOutput`); the old `JRExporterParameter` constants were removed upstream.

## Conventions

- The override hook is producer-agnostic: it works whether JasperReports uses the default OpenPDF "classic" producer (`jasperreports-pdf`) or iText 7 (`jasperreports-pdf-lib7`).
- The PUA codepoints assume a font that ships the legacy Thai PUA glyph set (the Acrobat/Adobe Thai layout). Many common Thai fonts include it — **TH Sarabun New does**, and `PdfRenderHarnessTest` confirms this library renders correctly with it (its `thai-library.pdf` carries PUA codepoints absent from the native output). A purely modern TTF lacking those PUA glyphs would render blank marks; for that case use the native `glyph.renderer.blocks.x=thai` property instead (see top of this file).
