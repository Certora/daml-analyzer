package com.certora.damlanalyzer.analysis

import com.certora.damlanalyzer.parse.DarLoader.LoadedDar
import com.certora.damlanalyzer.schema._
import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast

object CrossPackageAnalyzer {

  // The caller is either a
  // template choice (callerTemplate) or an interface choice (callerInterface).
  // it cannot be both.
  private case class RawFinding(
    module:          String,
    callerTemplate:  Option[String],
    callerInterface: Option[String],
    callerChoice:    Option[String],
    loc:             Option[Ref.Location],
    update:          Ast.Update
  )

  def analyze(dar: LoadedDar): AnalysisResult = {

    val (mainPkgId, mainPkg) = dar.main

    val mainMeta = mainPkg.metadata

    val pkgsById: Map[PackageId, Ast.Package] = dar.all.toMap

    val walkCtx = WalkCtx(pkgsById = pkgsById)

    val findings: List[RawFinding] =
      mainPkg.modules.toList.flatMap { case (modName, mod) =>
        val fromTemplates = mod.templates.toList.flatMap { case (tplName, tpl) =>
          findingsFromChoices(modName.toString, tplName.toString, isInterface = false, tpl.choices.values, walkCtx)
        }
        val fromInterfaces = mod.interfaces.toList.flatMap { case (ifaceName, iface) =>
          findingsFromChoices(modName.toString, ifaceName.toString, isInterface = true, iface.choices.values, walkCtx)
        }
        fromTemplates ++ fromInterfaces
      }

    // only cross-package interactions needed
    val crossPkg = findings.filter(f => targetTypeConId(f.update).exists(_.packageId != mainPkgId))

    val updateInteractions = crossPkg.flatMap { f =>
      for {
        tcid  <- targetTypeConId(f.update)
        iType <- interactionTypeOf(f.update)
      } yield {
        val targetPkg = pkgsById.get(tcid.packageId)
        val qualNm        = tcid.qualifiedName
        val isIface   = isInterfaceTarget(f.update)
        CrossPackageInteraction(
          interactionType = iType,
          source          = Some(f.loc.map(toSchemaLocation).getOrElse(fileOnlyLocation(f.module))),
          caller = Caller(
            pkg       = mainMeta.name.toString,
            version   = mainMeta.version.toString,
            packageId = mainPkgId.toString,
            module    = f.module,
            template  = f.callerTemplate,
            interface = f.callerInterface,
            choice    = f.callerChoice
          ),
          target = Target(
            pkg       = targetPkg.map(_.metadata.name.toString).getOrElse("?"),
            version   = targetPkg.map(_.metadata.version.toString).getOrElse("?"),
            packageId = tcid.packageId.toString,
            module    = qualNm.module.toString,
            template  = if (isIface) None else Some(qualNm.name.toString),
            interface = if (isIface) Some(qualNm.name.toString) else None,
            choice    = choiceOf(f.update),
            consuming = consumingOf(f.update, pkgsById)
          )
        )
      }
    }

    val implementsInterfaceInteractions = TemplateAnalyzer.findImplementsInterfaceFindings(dar)
    // dedup identical findings
    val interactions = (updateInteractions ++ implementsInterfaceInteractions).distinct

    val deps = dar.all.collect {
      case (id, pkg) if id != mainPkgId && !Stdlib.isStdlib(pkg.metadata.name.toString) =>
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

  // go over choices and generate `RawFinding`.
  private def findingsFromChoices(
    modName:     String,
    owner:       String,
    isInterface: Boolean,
    choices:     Iterable[Ast.TemplateChoice],
    walkCtx:     WalkCtx
  ): List[RawFinding] =
    choices.toList.flatMap { ch =>
      ExprWalker.findInteractions(ch.update, None, walkCtx).map { case (loc, updt) =>
        RawFinding(
          module          = modName,
          callerTemplate  = Option.when(!isInterface)(owner),
          callerInterface = Option.when(isInterface)(owner),
          callerChoice    = Some(ch.name.toString),
          loc             = loc,
          update          = updt
        )
      }
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
    case Ast.UpdateQueryNByKey(tcid)                   => Some(tcid)
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
    case _: Ast.UpdateQueryNByKey       => Some(InteractionType.QueryNByKey)
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

  // consuming for exercise variants — looked up on the target choice's definition.
  // Templates and interfaces both store choices with a consuming Boolean.
  private def consumingOf(updt: Ast.Update, pkgsById: Map[PackageId, Ast.Package]): Option[Boolean] = {
    def lookupTemplate(tcid: Ref.TypeConId, choice: Ref.ChoiceName): Option[Boolean] = {
      val qualNm = tcid.qualifiedName
      pkgsById.get(tcid.packageId)
        .flatMap(_.modules.get(qualNm.module))
        .flatMap(_.templates.get(qualNm.name))
        .flatMap(_.choices.get(choice))
        .map(_.consuming)
    }

    def lookupInterface(tcid: Ref.TypeConId, choice: Ref.ChoiceName): Option[Boolean] = {
      val qualNm = tcid.qualifiedName
      pkgsById.get(tcid.packageId)
        .flatMap(_.modules.get(qualNm.module))
        .flatMap(_.interfaces.get(qualNm.name))
        .flatMap(_.choices.get(choice))
        .map(_.consuming)
    }

    updt match {
      case Ast.UpdateExercise(tcid, choice, _, _)             => lookupTemplate(tcid, choice)
      case Ast.UpdateExerciseByKey(tcid, choice, _, _)        => lookupTemplate(tcid, choice)
      case Ast.UpdateExerciseInterface(tcid, choice, _, _, _) => lookupInterface(tcid, choice)
      case _                                                   => None
    }
  }

  // Convert a Daml-LF Ref.Location to our schema's SourceLocation.
  private def toSchemaLocation(loc: Ref.Location): SourceLocation =
    SourceLocation(
      file        = loc.module.toString + ".daml",
      startLine   = Some(loc.start._1 + 1),
      startColumn = Some(loc.start._2 + 1),
      endLine     = Some(loc.end._1 + 1),
      endColumn   = Some(loc.end._2 + 1)
    )

  private def fileOnlyLocation(modName: String): SourceLocation =
    SourceLocation(file = modName + ".daml")

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
