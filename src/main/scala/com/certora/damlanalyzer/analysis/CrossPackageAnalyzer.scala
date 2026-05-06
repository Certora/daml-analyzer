package com.certora.damlanalyzer.analysis

import com.certora.damlanalyzer.parse.DarLoader.LoadedDar
import com.certora.damlanalyzer.schema._
import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast

object CrossPackageAnalyzer {

  def analyze(dar: LoadedDar): AnalysisResult = {
    val (mainPkgId, mainPkg) = dar.main
    val mainMeta = mainPkg.metadata
    val pkgsById: Map[PackageId, Ast.Package] = dar.all.toMap

    // Each finding has the module and template/interface it came from.
    // We traverse three types of DValues:
    //   1. mod.definitions: module level DValues meaning top-level helpers and the
    //      `$$sc_<Template>_<N>` helpers Daml uses for choice bodies.
    //   2. mod.templates[*].choices[*].update: template choice bodies.
    //   3. mod.interfaces[*].choices[*].update: interface choice bodies.
    // For (1) the owning template is not known (maybe?) so `callerTpl` is None.
    val findings: List[(String, Option[String], Option[Ref.Location], Ast.Update)] =
      mainPkg.modules.toList.flatMap { case (modName, mod) =>
        val fromDefs = mod.definitions.values.toList.flatMap {
          case dv: Ast.GenDValue[_] =>
            ExprWalker.findInteractions(dv.body.asInstanceOf[Ast.Expr])
              .map { case (loc, u) => (modName.toString, None, loc, u) }
          case _ => Nil
        }
        val fromTemplates = mod.templates.toList.flatMap { case (tplName, tpl) =>
          tpl.choices.values.toList.flatMap { ch =>
            ExprWalker.findInteractions(ch.update.asInstanceOf[Ast.Expr])
              .map { case (loc, u) => (modName.toString, Some(tplName.toString), loc, u) }
          }
        }
        val fromInterfaces = mod.interfaces.toList.flatMap { case (ifaceName, iface) =>
          iface.choices.values.toList.flatMap { ch =>
            ExprWalker.findInteractions(ch.update.asInstanceOf[Ast.Expr])
              .map { case (loc, u) => (modName.toString, Some(ifaceName.toString), loc, u) }
          }
        }
        fromDefs ++ fromTemplates ++ fromInterfaces
      }

    // only cross-package interactions needed
    val crossPkg = findings.filter { case (_, _, _, u) =>
      targetTypeConId(u).exists(_.packageId != mainPkgId)
    }

    val updateInteractions = crossPkg.flatMap { case (modName, callerTpl, loc, u) =>
      for {
        tcid  <- targetTypeConId(u)
        iType <- interactionTypeOf(u)
      } yield {
        val targetPkg = pkgsById.get(tcid.packageId)
        val qn        = tcid.qualifiedName
        val isIface   = isInterfaceTarget(u)
        CrossPackageInteraction(
          interactionType = iType,
          source          = loc.map(toSchemaLocation),
          caller = Caller(
            pkg       = mainMeta.name.toString,
            version   = mainMeta.version.toString,
            packageId = mainPkgId.toString,
            module    = modName,
            template  = callerTpl
          ),
          target = Target(
            pkg       = targetPkg.map(_.metadata.name.toString).getOrElse("?"),
            version   = targetPkg.map(_.metadata.version.toString).getOrElse("?"),
            packageId = tcid.packageId.toString,
            module    = qn.module.toString,
            template  = if (isIface) None else Some(qn.name.toString),
            interface = if (isIface) Some(qn.name.toString) else None,
            choice    = choiceOf(u),
            consuming = consumingOf(u, pkgsById)
          )
        )
      }
    }

    val implementsInterfaceInteractions = TemplateAnalyzer.findImplementsInterfaceFindings(dar)
    val interactions = updateInteractions ++ implementsInterfaceInteractions

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
        lfVersion = mainPkg.languageVersion.pretty
      ),
      dependencies = deps,
      summary      = summaryOf(interactions),
      interactions = interactions
    )
  }

  // The TypeConId that identifies the cross-package target of an Update
  // either a template or an interface
  private def targetTypeConId(u: Ast.Update): Option[Ref.TypeConId] = u match {
    case Ast.UpdateExercise(tcid, _, _, _)             => Some(tcid)
    case Ast.UpdateExerciseInterface(tcid, _, _, _, _) => Some(tcid)
    case Ast.UpdateExerciseByKey(tcid, _, _, _)        => Some(tcid)
    case Ast.UpdateCreate(tcid, _)                     => Some(tcid)
    case Ast.UpdateCreateInterface(tcid, _)            => Some(tcid)
    case Ast.UpdateFetchTemplate(tcid, _)              => Some(tcid)
    case Ast.UpdateFetchInterface(tcid, _)             => Some(tcid)
    case Ast.UpdateFetchByKey(tcid)                    => Some(tcid)
    case Ast.UpdateLookupByKey(tcid)                   => Some(tcid)
    case _                                             => None
  }

  private def interactionTypeOf(u: Ast.Update): Option[InteractionType] = u match {
    case _: Ast.UpdateExercise          => Some(InteractionType.Exercise)
    case _: Ast.UpdateExerciseInterface => Some(InteractionType.ExerciseInterface)
    case _: Ast.UpdateExerciseByKey     => Some(InteractionType.ExerciseByKey)
    case _: Ast.UpdateCreate            => Some(InteractionType.Create)
    case _: Ast.UpdateCreateInterface   => Some(InteractionType.CreateInterface)
    case _: Ast.UpdateFetchTemplate     => Some(InteractionType.Fetch)
    case _: Ast.UpdateFetchInterface    => Some(InteractionType.FetchInterface)
    case _: Ast.UpdateFetchByKey        => Some(InteractionType.FetchByKey)
    case _: Ast.UpdateLookupByKey       => Some(InteractionType.LookupByKey)
    case _                              => None
  }

  private def isInterfaceTarget(u: Ast.Update): Boolean = u match {
    case _: Ast.UpdateExerciseInterface => true
    case _: Ast.UpdateCreateInterface   => true
    case _: Ast.UpdateFetchInterface    => true
    case _                              => false
  }

  private def choiceOf(u: Ast.Update): Option[String] = u match {
    case Ast.UpdateExercise(_, choice, _, _)             => Some(choice.toString)
    case Ast.UpdateExerciseInterface(_, choice, _, _, _) => Some(choice.toString)
    case Ast.UpdateExerciseByKey(_, choice, _, _)        => Some(choice.toString)
    case _                                               => None
  }

  // consuming  for exercise variants — looked up on the target choice's definition.
  // note that this only works for template-based exercises
  // Todo: ExerciseInterface later
  private def consumingOf(u: Ast.Update, pkgsById: Map[PackageId, Ast.Package]): Option[Boolean] = {
    def lookupTemplate(tcid: Ref.TypeConId, choice: Ref.ChoiceName): Boolean = {
      val qn = tcid.qualifiedName
      (for {
        pkg <- pkgsById.get(tcid.packageId)
        mod <- pkg.modules.get(qn.module)
        tpl <- mod.templates.get(qn.name)
        ch  <- tpl.choices.get(choice)
      } yield ch.consuming).getOrElse(false)
    }
    u match {
      case Ast.UpdateExercise(tcid, choice, _, _)      => Some(lookupTemplate(tcid, choice))
      case Ast.UpdateExerciseByKey(tcid, choice, _, _) => Some(lookupTemplate(tcid, choice))
      case _                                            => None
    }
  }

  // Convert a Daml-LF Ref.Location to our schema's SourceLocation.
  // File path: derived from module name (`.daml` suffix) — works for the standard
  // one-module-per-file convention; may not hold for unusual layouts.
  private def toSchemaLocation(loc: Ref.Location): SourceLocation =
    SourceLocation(
      file        = loc.module.toString + ".daml",
      startLine   = loc.start._1 + 1,
      startColumn = loc.start._2 + 1,
      endLine     = loc.end._1 + 1,
      endColumn   = loc.end._2 + 1
    )

  // some other helpers
  private def isStdlib(name: String): Boolean =
    name.startsWith("daml-prim") ||
      name.startsWith("daml-stdlib") ||
      name.startsWith("ghc-stdlib")

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
