package com.certora.damlanalyzer.analysis

import com.certora.damlanalyzer.parse.DarLoader.LoadedDar
import com.certora.damlanalyzer.schema._
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast

object CrossPackageAnalyzer {

  def analyze(dar: LoadedDar): AnalysisResult = {
    val (mainPkgId, mainPkg) = dar.main
    val mainMeta = mainPkg.metadata
    val pkgsById: Map[PackageId, Ast.Package] = dar.all.toMap

    // get all exercises in the main pkg and the module they are in
    val findings: List[(String, Ast.UpdateExercise)] =
      mainPkg.modules.toList.flatMap { case (modName, mod) =>
        mod.definitions.values.toList.flatMap {
          case dv: Ast.GenDValue[_] =>
            ExprWalker.findExercises(dv.body.asInstanceOf[Ast.Expr])
              .map(modName.toString -> _)
          case _ => Nil
        }
      }

    val crossPkg = findings.filter { case (_, ex) =>
      ex.templateId.packageId != mainPkgId
    }

    val interactions = crossPkg.map { case (modName, ex) =>
      val targetId  = ex.templateId.packageId
      val targetPkg = pkgsById.get(targetId)
      val qn        = ex.templateId.qualifiedName
      CrossPackageInteraction(
        interactionType = InteractionType.Exercise,
        source          = None, // todo locations to add later
        caller = Caller(
          pkg       = mainMeta.name.toString,
          version   = mainMeta.version.toString,
          packageId = mainPkgId.toString,
          module    = modName
        ),
        target = Target(
          pkg       = targetPkg.map(_.metadata.name.toString).getOrElse("?"),
          version   = targetPkg.map(_.metadata.version.toString).getOrElse("?"),
          packageId = targetId.toString,
          module    = qn.module.toString,
          template  = qn.name.toString,
          choice    = ex.choice.toString,
          consuming = consumingOf(ex, pkgsById)
        )
      )
    }

    val deps = dar.all.collect {
      case (id, pkg) if id != mainPkgId && !isStdlib(pkg.metadata.name.toString) =>
        PackageRef(
          name      = pkg.metadata.name.toString,
          version   = pkg.metadata.version.toString,
          packageId = id.toString
        )
    }.toList

    AnalysisResult(
      analyzedPackage = AnalyzedPackage(
        name      = mainMeta.name.toString,
        version   = mainMeta.version.toString,
        packageId = mainPkgId.toString,
        lfVersion = mainPkg.languageVersion.toString
      ),
      dependencies = deps,
      summary      = summaryOf(interactions),
      interactions = interactions
    )
  }

  private def isStdlib(name: String): Boolean =
    name.startsWith("daml-prim") ||
      name.startsWith("daml-stdlib") ||
      name.startsWith("ghc-stdlib")

  private def consumingOf(
    ex: Ast.UpdateExercise,
    pkgsById: Map[PackageId, Ast.Package]
  ): Boolean = {
    val qn = ex.templateId.qualifiedName
    (for {
      pkg <- pkgsById.get(ex.templateId.packageId)
      mod <- pkg.modules.get(qn.module)
      tpl <- mod.templates.get(qn.name)
      ch  <- tpl.choices.get(ex.choice)
    } yield ch.consuming).getOrElse(false)
  }

  private def summaryOf(interactions: List[CrossPackageInteraction]): Summary = {
    val byType = interactions
      .groupBy(_.interactionType.toString)
      .view.mapValues(_.size).toMap
    val byTarget = interactions
      .groupBy(_.target.pkg)
      .view.mapValues(_.size).toMap
    Summary(interactions.size, byType, byTarget)
  }
}
