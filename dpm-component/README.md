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
  - oci://ghcr.io/certora/daml-analyzer:0.1.3
```

Then:

```bash
dpm install package
dpm certora-analyze foo.dar -o out/
open out/foo.html
```

`out/` also contains `foo.json` and `foo.dot`. The batch mode, i.e., `dpm certora-analyze dars-dir/ -o out/`, generates a `.json` and `.dot` for each DAR, and one `out/report.html` for all DARs.

## For maintainers: build + publish

Tagged releases are published automatically: push a `v<version>` tag and [`.github/workflows/publish-component.yml`](../.github/workflows/publish-component.yml) builds the fat JAR and pushes `oci://ghcr.io/certora/daml-analyzer:<version>` using the workflow's GHCR credentials.

To sanity-check the archive locally before tagging, requires DPM 3.5+ (`dpm install 3.5.1` or later):

```bash
./scripts/build-dpm-component.sh   # assembles the fat JAR into dpm-component/lib/
dpm publish component oci://ghcr.io/certora/daml-analyzer:<version> --platform generic="./dpm-component" --dry-run
```
