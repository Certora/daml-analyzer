package com.certora.damlanalyzer.analysis

import com.certora.damlanalyzer.parse.DarLoader.LoadedDar
import com.certora.damlanalyzer.schema._
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast

object TemplateAnalyzer {

  // Go over a package's templates and find all `ImplementsInterface``
  // for each template.implements entry whose interface lives in another package.
  def findImplementsInterfaceFindings(dar: LoadedDar): List[CrossPackageInteraction] = {
    val (mainPkgId, mainPkg) = dar.main
    val mainMeta = mainPkg.metadata
    val pkgsById: Map[PackageId, Ast.Package] = dar.all.toMap

    mainPkg.modules.toList.flatMap { case (modName, mod) =>
      mod.templates.toList.flatMap { case (tplName, tpl) =>
        tpl.implements.keys.toList.collect {
          case ifaceId if ifaceId.packageId != mainPkgId =>
            val targetPkg = pkgsById.get(ifaceId.packageId)
            val qn        = ifaceId.qualifiedName
            CrossPackageInteraction(
              interactionType = InteractionType.ImplementsInterface,
              source          = Some(SourceLocation(file = modName.toString + ".daml")),
              caller = Caller(
                pkg       = mainMeta.name.toString,
                version   = mainMeta.version.toString,
                packageId = mainPkgId.toString,
                module    = modName.toString,
                template  = Some(tplName.toString)
              ),
              target = Target(
                pkg       = targetPkg.map(_.metadata.name.toString).getOrElse("?"),
                version   = targetPkg.map(_.metadata.version.toString).getOrElse("?"),
                packageId = ifaceId.packageId.toString,
                module    = qn.module.toString,
                template  = None,
                interface = Some(qn.name.toString),
                choice    = None,
                consuming = None
              )
            )
        }
      }
    }
  }
}
