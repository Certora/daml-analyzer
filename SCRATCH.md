# Issues to Handle

### Source line numbers are missing
We always populate `source.file` based on the caller's module, but `startLine`/`endLine` are not present when no `ELocation` is reachable. We see this in  Splice for 1339/2415 findings. Based on discussion with Canton developers, it looks like not every line of `lf` can be directly correlated to a line in `daml` due to two reasons:
- `lf` being generated, think typeclass instances
- code being optimized/transformed, since DAML utilizes GHC

So for now, we may not be able to fix much.

### Higher-order functions

We resolve direct `EVal(qname)` references but not indirect calls where the function being called is computed dynamically, e.g. `customHigherOrderHelper (\x -> exercise x ...)` with a non-stdlib HOF. I think stdlib HOFs like `mapM_`/`forA_` will work because the lambda argument is inline at the call site and we do go inside it via `EAbs`.

TODO: we can do it if we see a need in real applications. Probably need something like k-CFA.

### ByKey detection has no integration test

We do have support for detecting `ExerciseByKey`/`FetchByKey`/`LookupByKey` but when trying to use these we saw that contract keys were removed in Daml-LF 2.x. We might just keep it for legacy LF 1.x DARs though.