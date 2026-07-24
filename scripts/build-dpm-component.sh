#!/usr/bin/env bash
set -e
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB="$REPO/dpm-component/lib"
(cd "$REPO" && sbt -batch assembly)
mkdir -p "$LIB"
cp "$REPO"/target/scala-2.13/daml-analyzer-*.jar "$LIB/daml-analyzer.jar"
