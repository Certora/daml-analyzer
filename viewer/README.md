# daml-analyzer viewer

Browser-based summary table for the analyzer's JSON output.

> Built with [Claude Code](https://claude.com/claude-code).

## Usage

There are two ways to view analyzer results:

**1. Open a self-contained HTML report** (recommended, no upload step)

Run the analyzer with `-o out/`. It generates a `out/<name>.html` for a single DAR, or `out/report.html` for all DARs in batch mode. Just:

```bash
open out/foo.html          # for a single DAR
open out/report.html       # batch mode aggregating all DARs
```

The HTML reports have the JSON payload embedded.

**2. Open this viewer and upload JSON(s) manually**

```bash
open viewer/index.html
```

Pick one or more `.json` files from an `out/` dir. You can select more than one. Note: `viewer/index.html` is a symlink to `src/main/resources/viewer/index.html`, which is bundled with the JAR and reused by mode (1).

## What it shows
There are 4 tabs.

### Highlights
Main findings from the analysis

### Summary Table
One section per loaded JSON. Rows = target packages, columns = interaction-type counts. Example:

```
my-app           1.0.0 · 50 interactions across 3 target packages

Target package   Version  Create  Exercise  Fetch  ExerciseInterface  Total
foo-registry     0.4.1    1       20        2      -                  23
bar-holding      0.1.2    -       8         1      4                  13
baz-credential   0.0.4    -       -         11     3                  14
                 Total    1       28        14     7                  50
```

- **Click a row** → right panel lists every finding targeting that package, with caller template/choice, source `file:line:col`, and consuming pill.
- **Click a single count cell** → narrows to just that interaction type for that target.
- **Search box** filters rows by package-name substring.

### Graph
A graphical view of the summary, but this is less detailed. You can click on the nodes to get more information.

### Diff
Shows what changed between two versions of a DAR.
