#!/usr/bin/env bash
set -e
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
(cd "$REPO" && sbt -batch assembly)
cp "$REPO"/target/scala-2.13/daml-analyzer-*.jar "$REPO/dpm-component/lib/daml-analyzer.jar"
