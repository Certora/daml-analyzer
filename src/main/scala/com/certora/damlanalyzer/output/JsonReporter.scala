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

  private def interactionJson(i: CrossPackageInteraction): String = {
    val callerCommon = List(
      s""""package": ${q(i.caller.pkg)}""",
      s""""version": ${q(i.caller.version)}""",
      s""""packageId": ${q(i.caller.packageId)}""",
      s""""module": ${q(i.caller.module)}"""
    )
    val callerOptional =
      i.caller.template.map(t => s""""template": ${q(t)}""").toList
    val callerFields = (callerCommon ++ callerOptional).mkString(",\n        ")

    val targetCommon = List(
      s""""package": ${q(i.target.pkg)}""",
      s""""version": ${q(i.target.version)}""",
      s""""packageId": ${q(i.target.packageId)}""",
      s""""module": ${q(i.target.module)}"""
    )
    val targetOptional =
      i.target.template.map(t  => s""""template": ${q(t)}""").toList ++
      i.target.interface.map(t => s""""interface": ${q(t)}""").toList ++
      i.target.choice.map(c    => s""""choice": ${q(c)}""").toList ++
      i.target.consuming.map(c => s""""consuming": $c""").toList
    val targetFields = (targetCommon ++ targetOptional).mkString(",\n        ")

    val sourceJson = i.source.map { s =>
      s""""source": {
        "file": ${q(s.file)},
        "startLine": ${s.startLine},
        "startColumn": ${s.startColumn},
        "endLine": ${s.endLine},
        "endColumn": ${s.endColumn}
      },
      """
    }.getOrElse("")

    s"""    {
      "type": ${q(i.interactionType)},
      $sourceJson"caller": {
        $callerFields
      },
      "target": {
        $targetFields
      }
    }"""
  }

  private def q(s: Any): String = "\"" + s.toString + "\""
}
