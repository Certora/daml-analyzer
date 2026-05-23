# daml-analyzer

Static analysis tool for inspecting cross-package interactions in compiled Daml packages (`.dar` files).

## Build

```bash
sbt compile           
sbt assembly # makes jar
```

If you make the JAR file, run like so:

```bash
java -jar target/scala-2.13/daml-analyzer-0.1.0-SNAPSHOT.jar foo.dar -o out/
```

See the various ways to run below. You can either run `sbt "run ..."` or `java -jar ...`.

## Run

| Command | Output |
|---|---|
| `sbt "run foo.dar"` | JSON to stdout |
| `sbt "run foo.dar -f dot"` | DOT to stdout |
| `sbt "run foo.dar -o out/"` | Writes both `out/foo.json` and `out/foo.dot` (default when `-o` is set) |
| `sbt "run foo.dar -f json -o out/"` | Only `out/foo.json` |
| `sbt "run foo.dar -f dot -o out/"` | Only `out/foo.dot` |
| `sbt "run dars-dir/ -o out/"` | For each `*.dar` in `dars-dir`, both `<name>.json` and `<name>.dot` in `out/` |
| `sbt "run dars-dir/"` | Error because batch mode requires `-o` |
| `sbt "run --help"` | Print usage |

## Visualize the results

Two options depending on the output format you produced.

**JSON → browser-based summary table** 

Open `viewer/index.html` in any browser, click the file input, and pick one or more `.json` files from your output directory. See [viewer/README.md](viewer/README.md) for details.

```bash
sbt "run foo.dar -o out/"
open viewer/index.html  # load out/foo.json using file picker
```

**DOT → PNG with Graphviz**

```bash
dot -Tpng path/to/foo.dot -o path/to/foo.png
```

## Test

```bash
sbt test
```