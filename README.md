# daml-analyzer

[![CI](https://github.com/Certora/daml-analyzer/actions/workflows/ci.yml/badge.svg)](https://github.com/Certora/daml-analyzer/actions/workflows/ci.yml)

A Static analysis tool for inspecting cross-package interactions in compiled Daml packages (`.dar` files).

<p align="center">
  <img src="docs/screenshots/daml1.png" alt="Highlights tab showing an overview of all interactions and choices." width="900">
  <br>
  <em>Highlights tab showing an overview of all interactions and choices.</em>
</p>

## What it does

Given a compiled Daml package, daml-analyzer reports all **cross-package interaction** the code performs against contracts or interfaces defined in other packages — `Create`, `Exercise`, `Fetch`, `ExerciseInterface`, `ImplementsInterface`, and so on.
The tool tracks the caller choice that triggered it, the target package, module, template/interface and choice it reaches, whether the choice is consuming, and the source `file:line:col` (when available). The output is a JSON, a Graphviz DOT, and an HTML report.

The HTML report bundles the [**viewer**](viewer/) with the JSON already embedded. You can open it in any browser. See [viewer/README.md](viewer/README.md) for the standalone viewer.

## Who this is for

Developers building protocols for Canton and auditors reviewing Canton deployments.

## Status

This project is under active development.

We have successfully run daml-analyzer on the [full Splice corpus](https://github.com/canton-network/splice/tree/main/daml/dars) (165 DARs) and all the files in the [utilities](https://docs.digitalasset.com/utilities/mainnet/reference/dar-versions/dar-versions.html) (53 DARs).

We would love to hear from users and get feedback from the community.

## Build

```bash
sbt compile           
sbt assembly # makes jar
```

## Run

There are 3 ways to run the tool:

1. **`dpm certora-analyze foo.dar -o out/`**: once the tool is installed as a DPM component. This is recommended for Canton/Daml users who already have DPM 3.5+. See [dpm-component/README.md](dpm-component/README.md) for more.
2. **`java -Xss4m -jar target/scala-2.13/daml-analyzer-<version>.jar foo.dar -o out/`**: for anyone who wants to run the jar directly
3. **`sbt "run foo.dar -o out/"`**: for developing on this repo.

See the various ways to run below. You can either run `sbt "run ..."` or `java -jar ...`.

| Command | Output |
|---|---|
| `sbt "run dars-dir/ -o out/"` | For each `*.dar` in `dars-dir`: `<name>.{json,dot}` in `out/` and one aggregated `out/report.html` for all DARs |
| `sbt "run foo.dar -o out/"` | Writes `out/foo.{json,dot,html}` (default `-f all`) |
| `sbt "run foo.dar -f html"` | HTML report to stdout |
| `sbt "run foo.dar -f json"` | JSON to stdout |
| `sbt "run foo.dar -f dot"` | DOT to stdout |
| `sbt "run foo.dar -f html -o out/"` | Only `out/foo.html` |
| `sbt "run dars-dir/"` | Error because batch mode requires `-o` |
| `sbt "run --help"` | Print usage |

If you make the JAR file, run like so:

```bash
java -jar target/scala-2.13/daml-analyzer-0.1.2.jar foo.dar -o out/
```

_NOTE:_ you can pass `-Xss4m` to the `java ...` command to increase the JVM thread stack. `sbt run` and `sbt test` already uses this flag (`build.sbt`).


## Visualize the results

Two options depending on the output format you produced.

**HTML report (recommended)**

```bash
sbt "run foo.dar -o out/"
open out/foo.html                     # single DAR
open out/report.html                  # batch mode: all DARs in one aggregated report
```

As a concrete example, run daml-analyzer on the Splice dar files [here](https://github.com/canton-network/splice/tree/main/daml/dars):

1. clone the splice repo
2. run `sbt assembly`
3. run `sbt "run path/to/splice/dars -o path/to/out/dir"`
4. `open path/to/out/dir/report.html`

**Manual JSON upload (fallback)**

If you produced JSON without HTML (`-f json`), open `viewer/index.html` in any browser and pick the `.json` files with the file input. See [viewer/README.md](viewer/README.md).

<p align="center">
  <img src="docs/screenshots/daml2.png" alt="Summary table tab showing target packages and interaction-type counts with a row clicked to reveal the grouped findings panel" width="900">
  <br>
  <em>Summary table: rows are target packages, columns are interaction-type counts. Click a row to see findings grouped by the choice that triggered them.</em>
</p>

<p align="center">
  <img src="docs/screenshots/daml3.png" alt="Diff tab showing a star graph of what changed between two versions of the same package, with green/red/amber/gray edges indicating added, removed, changed, and unchanged interactions" width="900">
  <br>
  <em>Diff tab: comparing two versions of the same package. Edges are <strong>green</strong> for added interactions, <strong>red dashed</strong> for removed, <strong>amber</strong> for changed, and gray for unchanged.</em>
</p>


**DOT → PNG with Graphviz**

```bash
dot -Tpng path/to/foo.dot -o path/to/foo.png
```

## Test

```bash
sbt test
```

### AI Acknowledgement
Claude Code, Codex, Gemini, Copilot, and ChatGPT were used for assistance at various points. All suggestions were reviewed by the authors.
