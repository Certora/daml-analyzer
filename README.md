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