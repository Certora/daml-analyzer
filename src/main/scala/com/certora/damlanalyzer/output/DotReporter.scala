package com.certora.damlanalyzer.output

import com.certora.damlanalyzer.schema._

// TODO: may get rid of this code if we decide to go with interactive viewer

// rendering an AnalysisResult as DOT file.
// it is expected to show one edge per (caller pkg, target pkg, interaction type, consuming flag) tuple,
// labelled with a count.
object DotReporter {

  def render(result: AnalysisResult): String = {
    val mainName = result.analyzedPackage.name
    val mainVer  = result.analyzedPackage.version
    val deps     = result.dependencies.map(d => d.name -> d.version).toMap

    // main package + every dependency that appears in interactions or the dep list
    val nodeNames = (Set(mainName) ++ result.dependencies.map(_.name)).toList.sorted

    val nodeDecls = nodeNames.map { name =>
      val version = if (name == mainName) mainVer else deps.getOrElse(name, "?")
      val color   = if (name == mainName) "\"#e3f2fd\"" else "\"#ffffff\""
      s"""  ${nodeId(name)} [label="$name\\n$version", fillcolor=$color];"""
    }.mkString("\n")

    // aggregating edges by (caller, target, type, consuming) tuple
    val edges = result.interactions
      .groupBy { i => EdgeKey(i.caller.pkg, i.target.pkg, i.interactionType.toString, i.target.consuming)}
      .view
      .mapValues(_.size)
      .toMap

    val edgeDecls = edges.toList
      .sortBy { case (k, _) => (k.callerPkg, k.targetPkg, k.iType) }
      .map { case (key, count) =>
        val consumingNote = key.consuming match {
          case Some(true)  => " (consuming)"
          case Some(false) => " (non-consuming)"
          case None        => ""
        }

        val label = s"$count× ${key.iType}$consumingNote"

        // non-consuming is the unusual case (consuming is the default for `choice`),
        // so highlight non-consuming in red.
        val color =
          if (key.consuming.contains(false)) {
            "\"#d32f2f\""
          } else {
            "\"#666666\""
          }
        s"""  ${nodeId(key.callerPkg)} -> ${nodeId(key.targetPkg)} [label="$label", color=$color];"""

      }.mkString("\n")

    s"""digraph daml_analyzer {
  rankdir=LR;
  node [shape=box, style="rounded,filled", fontname="Helvetica"];
  edge [fontname="Helvetica"];

$nodeDecls

$edgeDecls
}"""
  }

  private case class EdgeKey(
    callerPkg: String,
    targetPkg: String,
    iType:     String,
    consuming: Option[Boolean]
  )

  private def nodeId(pkgName: String): String = "\"" + pkgName + "\""
}
