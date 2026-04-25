package com.certora.damlanalyzer.schema

case class PackageRef(name: String, version: String, packageId: String)

case class AnalyzedPackage(
  name: String,
  version: String,
  packageId: String,
  lfVersion: String
)

case class SourceLocation(
  file: String,
  startLine: Int,
  startColumn: Int,
  endLine: Int,
  endColumn: Int
)

case class Caller(
  pkg: String,
  version: String,
  packageId: String,
  module: String
)

case class Target(
  pkg: String,
  version: String,
  packageId: String,
  module: String,
  template: String,
  choice: String,
  consuming: Boolean
)

case class CrossPackageInteraction(
  interactionType: InteractionType,
  source: Option[SourceLocation],
  caller: Caller,
  target: Target
)

case class Summary(
  totalInteractions: Int,
  byType: Map[String, Int],
  byTargetPackage: Map[String, Int]
)

case class AnalysisResult(
  analyzedPackage: AnalyzedPackage,
  dependencies: List[PackageRef],
  summary: Summary,
  interactions: List[CrossPackageInteraction]
)
