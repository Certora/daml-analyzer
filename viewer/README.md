# daml-analyzer viewer

Browser-based summary table for the analyzer's JSON output.

> Built with [Claude Code](https://claude.com/claude-code).

## Usage

```bash
open viewer/index.html
```

Pick one or more `.json` files from running the tool.
Multi-select is supported, you can upload more than one json at a time.

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
