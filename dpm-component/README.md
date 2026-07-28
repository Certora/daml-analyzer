# daml-analyzer as a DPM component

Packages `daml-analyzer` as a [DPM component](https://github.com/canton-network-devs/Reference-DPM-Component) so users on **DPM 3.5+** can invoke it as `dpm certora-analyze`.

## Layout

```
dpm-component/
├── component.yaml        # declares the `certora-analyze` subcommand
├── daml.yaml             # stub required by dpm publish
├── LICENSE               # required at component root by dpm publish
├── NOTICE                # attribution
├── bin/
│   └── daml-analyzer.sh  # exec java -Xss4m -jar lib/daml-analyzer.jar "$@"
└── lib/
    └── daml-analyzer.jar # populated by scripts/build-dpm-component.sh
```

## For users

Add to your `daml.yaml`:

```yaml
name: my-project
source: daml
version: 0.0.1
dependencies:
  - daml-prim
  - daml-stdlib
components:
  - oci://ghcr.io/certora/daml-analyzer:0.1.1
```

Then:

```bash
dpm install package
dpm certora-analyze foo.dar -o out/
```

Load `out/foo.json` into [`viewer/index.html`](../viewer/index.html) to browse findings.

## For maintainers: build + publish

Requires DPM 3.5+ (`dpm install 3.5.1` or later) and a GHCR token with `write:packages` scope on the target org.

```bash
# 1. Assemble the JAR into dpm-component/lib/
./scripts/build-dpm-component.sh

# 2. Sanity check the archive locally
dpm publish component oci://ghcr.io/certora/daml-analyzer:0.1.1 --platform generic="./dpm-component" --dry-run

# 3. Push to GHCR, this needs PAT with write:packages
echo <PAT> | docker login ghcr.io -u <your-gh-user> --password-stdin
dpm publish component oci://ghcr.io/certora/daml-analyzer:0.1.1 --platform generic="./dpm-component"
```

After the first push, change the package visibility to public in GitHub -> org -> Packages.
