package com.certora.damlanalyzer.output

import com.certora.damlanalyzer.schema._

// TODO probably need to clean this up later...
object JsonReporter {

  def render(result: AnalysisResult): String = {
    val deps         = result.dependencies.map(depJson).mkString(",\n")
    val interactions = result.interactions.map(interactionJson).mkString(",\n")
    val byType       = result.summary.byType.map { case (k, v) => s"${q(k)}: $v" }.mkString(", ")
    val byPkg        = result.summary.byTargetPackage.map { case (k, v) => s"${q(k)}: $v" }.mkString(", ")

    s"""{
  "analyzedPackage": {
    "name": ${q(result.analyzedPackage.name)},
    "version": ${q(result.analyzedPackage.version)},
    "packageId": ${q(result.analyzedPackage.packageId)},
    "lfVersion": ${q(result.analyzedPackage.lfVersion)}
  },
  "dependencies": [
$deps
  ],
  "summary": {
    "totalInteractions": ${result.summary.totalInteractions},
    "byType": {$byType},
    "byTargetPackage": {$byPkg}
  },
  "interactions": [
$interactions
  ]
}"""
  }

  private def depJson(d: PackageRef): String = s"""    {
      "name": ${q(d.name)},
      "version": ${q(d.version)},
      "packageId": ${q(d.packageId)}
    }"""

  private def interactionJson(i: CrossPackageInteraction): String = s"""    {
      "type": ${q(i.interactionType)},
      "caller": {
        "package": ${q(i.caller.pkg)},
        "version": ${q(i.caller.version)},
        "packageId": ${q(i.caller.packageId)},
        "module": ${q(i.caller.module)}
      },
      "target": {
        "package": ${q(i.target.pkg)},
        "version": ${q(i.target.version)},
        "packageId": ${q(i.target.packageId)},
        "module": ${q(i.target.module)},
        "template": ${q(i.target.template)},
        "choice": ${q(i.target.choice)},
        "consuming": ${i.target.consuming}
      }
    }"""

  private def q(s: Any): String = "\"" + s.toString + "\""
}
