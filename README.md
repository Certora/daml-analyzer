# daml-analyzer

Static analysis tool for inspecting cross-package interactions in compiled Daml packages (`.dar` files).

## Build

```bash
sbt compile
```

## Run

```bash
# JSON will be dumped on stdout
sbt "run <dar-file>"

# JSON will be dumped in a file
sbt "run <dar-file> -o report.json"

# DOT will be dumped in a file, then you can render to PNG
sbt "run <dar-file> -f dot -o report.dot"
dot -Tpng report.dot -o report.png

# Help
sbt "run --help"
```

## Test

```bash
sbt test
```
### Interface exercises invoked via `exercise cid Choice` are not currently detected

When you write `exercise cid Choice` in Daml where `cid : ContractId Interface`, the Daml compiler desugars this into a typeclass dictionary call, i.e., a function reference (e.g. `pkg-iface:Mod:$cexercise1`) and relevant arguments. The actual `Update.ExerciseInterface` AST node ends up inside helper functions in the interface's package, not in the calling package's modules.

Since our tool currently traverses only the analyzed main package's modules, it sees the function call (`EApp(EVal(otherPkg:...))`) but cannot follow it into the dependency to find the underlying exercise.

As a result, cross-package interface exercises (e.g. `pkg-impl/Wallet.BurnToken` exercising `IToken.Burn`) are not currently surfaced as findings. The `ImplementsInterface` finding, which is structural, not behavioral, works correctly.

A regression test (`AnalyzerTests` test2b) currently fails for this reason — it asserts the *desired* behavior (an `ExerciseInterface` finding for `Wallet.BurnToken`) with a clear failure message, so once call-graph analysis lands the test will start passing automatically.

Solution: a control-flow analysis will fix this. When we hit a cross-package `EVal`, we can look up the called value's body in the dependency package and traverse it.

### Source locations are best-effort

We extract source locations from `Ast.ELocation` wrappers in the AST and show them as the `source` field on each finding. This is shown as file, line, column. The Daml compiler emits `ELocation` for user-written expressions like signatories, choice bodies, etc., but not for all compiler-generated synthetic helpers (e.g., `$$sc_<Template>_N`).

For the minimal `pkg-vault` example, the cross-package `exercise` is contained entirely in a synthetic helper that has no `ELocation` wrapping at any level and so the `source` field is omitted on that finding. Real-world Daml code typically has more user-written intermediate code where locations are present, so this gap is more pronounced on minimal test fixtures than on production codebases.

Solution: walk via template/choice structure so each finding inherits the choice's known source line or do call-graph attribution to use the caller's location when the helper has none.