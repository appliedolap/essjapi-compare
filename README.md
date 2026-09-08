# essjapi-compare

Generates a report of how the Essbase Java API has changed over time.

Point it at a collection of `ess_japi.jar` files — one per Essbase release — and it compares each
version against the one before it, then writes a single self-contained HTML page describing what
was added, removed, modified, or newly deprecated at every step.

The generated page is published straight from this repository at
**<https://appliedolap.github.io/essjapi-compare/>**. It needs no server, build step, or assets
alongside it.

## Why

The Essbase Java API has been shipped for over two decades with no public changelog. If you
maintain code against it — a connector, an outline extractor, a utility — the practical questions
are always the same: when did this method appear, when was it deprecated, and is the class I want
present in the version my customer is running? Comparing jars by hand does not scale to a hundred
releases, so this does it mechanically.

Under the hood it uses [japicmp](https://siom79.github.io/japicmp/) to diff the public API of
successive jars, and [Thymeleaf](https://www.thymeleaf.org/) to render the result.

## The Essbase jars are not in this repository

`ess_japi.jar` is Oracle-licensed and is not redistributable, so no Essbase jar is committed here
and none ever should be. You supply your own collection, from your own licensed installations. Only
the generated HTML is intended for publishing.

## Building

Requires JDK 17 or newer and Maven.

```bash
mvn clean package
```

That produces two artifacts in `target/`:

| Artifact | Use |
| --- | --- |
| `essjapi-compare-1.0.0-SNAPSHOT.jar` | The library, if you want to call the analyzer yourself |
| `essjapi-compare-1.0.0-SNAPSHOT-cli.jar` | Runnable, dependencies included |

## Running

```bash
java -jar target/essjapi-compare-1.0.0-SNAPSHOT-cli.jar --help
```

Two collection layouts are supported.

**One folder per version** — the layout Essbase collections usually end up in, and the one to prefer:

```
jars/
  9.3.1/ess_japi.jar
  11.1.2.4.048/ess_japi.jar
  21.8.2.0.0.031/ess_japi.jar
```

```bash
java -jar target/essjapi-compare-1.0.0-SNAPSHOT-cli.jar \
  --version-folders \
  --base-folder jars \
  --output-file report.html
```

Add `--jar-name` if the jar inside each folder is called something other than `ess_japi.jar`.

**All jars in one folder**, with the version in the filename:

```
jars/
  ess_japi-9.3.1.jar
  ess_japi-11.1.2.4.048.jar
```

```bash
java -jar target/essjapi-compare-1.0.0-SNAPSHOT-cli.jar \
  --base-folder jars \
  --output-file report.html
```

Add `--prefix` if the part before the version number is not `ess_japi-`.

Versions are ordered numerically, component by component, not lexically — so `9.3.1` sorts before
`11.1.2` and `11.1.2.4.010` before `11.1.2.4.014`.

## Generating the published report

`scripts/generate-japi-report.sh` wraps the CLI for the real-world case where the jars are spread
across several collections using inconsistent filenames. It merges any number of source folders
into one staging tree of symlinks with canonical names, reports the version coverage it found, and
runs the comparison over the result. Nothing is copied and the sources are never touched.

```bash
scripts/generate-japi-report.sh \
  ~/jars/older-releases \
  ~/jars/newer-releases
```

Useful options:

- `--dry-run` — stage and report coverage without generating, to see what a run would cover
- `--only 9.3.0,11.1.2,21.8.2.0.0.031` — restrict to a few versions, which turns a full run into a
  fast loop when the thing being changed is the template rather than the analysis
- `--output FILE` — defaults to `build/essbase-java-api-evolution/index.html`

Run `scripts/generate-japi-report.sh --help` for the rest.

## The output

One self-contained page: a version index up top, then a section per version with what changed
below it, in four categories — added, modified, removed, and deprecated. New methods are green,
removed methods red, and deprecated methods struck through. A version whose public API did not
change says so rather than being omitted.

Styling is [Pico CSS](https://picocss.com/) loaded from a CDN with a short list of local rules, and
the page follows the reader's light or dark preference.

The page carries no timestamp, deliberately: regenerating it from an unchanged collection produces
an identical file, so a rebuild that changes nothing shows up as no diff.

## Publishing

The committed report lives in `docs/`, which GitHub Pages serves from the default branch. So
publishing an update is just regenerating and committing:

```bash
scripts/generate-japi-report.sh /path/to/jars
git add docs/index.html && git commit -m "Regenerate the report"
```

There is no branch to switch to and no CI step. Because the page carries no timestamp, a
regeneration that found nothing new produces no diff at all.

## Layout

```
src/main/java/…/JapiCompare.java      CLI entry point
                JapiAnalyzer.java     Walks the collection, drives japicmp, renders the template
                ApiChanges.java       Queries over japicmp's model — what changed, and how
                MethodChange.java     One method's change, as the template needs it
                ChangeSet.java        One version-to-version comparison
                Version.java          Essbase version numbers, and their ordering
src/main/resources/templates/         The Thymeleaf template
scripts/generate-japi-report.sh       Multi-collection wrapper
docs/index.html                       The published report, served by GitHub Pages
```

## Tests

```bash
mvn test
```

The suite covers version parsing and ordering, the `java.lang` shortening rules, the collection
walk in both layouts, and the change queries — the last by fabricating classes with
[Javassist](https://www.javassist.org/) so japicmp has something to diff without needing any
Essbase jar present.

## License

Copyright 2017-2026 Applied OLAP, Inc., licensed under the
[Apache License 2.0](LICENSE).

Essbase is a trademark of Oracle Corporation. This project is not affiliated with or endorsed by
Oracle.
