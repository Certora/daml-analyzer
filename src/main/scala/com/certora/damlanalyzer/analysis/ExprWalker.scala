package com.certora.damlanalyzer.analysis

import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast

// Context for analyzing EVals.
// `pkgsById`:mlook up the body of a referenced DValue either in the main package or in any dependency.
// `visited`: don't recurse into the same DValue twice on a single path.
case class WalkCtx(
    pkgsById: Map[PackageId, Ast.Package] = Map.empty,
    visited:  Set[Ref.ValueRef]           = Set.empty
)

object ExprWalker {

  // TODO: still missing some Expr variants like ERecCon, EStructCon, EVariantCon, EThrow, ETryCatch, and stuff
  def findInteractions(
      e:          Ast.Expr,
      currentLoc: Option[Ref.Location] = None,
      ctx:        WalkCtx              = WalkCtx()
  ): List[(Option[Ref.Location], Ast.Update)] = e match {

    case Ast.EUpdate(u) => findInteractionsInUpdate(u, currentLoc, ctx)

    case Ast.ELocation(loc, body) => findInteractions(body, Some(loc), ctx)

    case Ast.EAbs(_, body)     => findInteractions(body, currentLoc, ctx)
    case Ast.ETyAbs(_, body)   => findInteractions(body, currentLoc, ctx)
    case Ast.ETyApp(e2, _)     => findInteractions(e2, currentLoc, ctx)
    case Ast.ESome(_, body)    => findInteractions(body, currentLoc, ctx)
    case Ast.EToAny(_, body)   => findInteractions(body, currentLoc, ctx)
    case Ast.EFromAny(_, body) => findInteractions(body, currentLoc, ctx)

    case Ast.EApp(fn, arg) =>
      findInteractions(fn, currentLoc, ctx) ++ findInteractions(arg, currentLoc, ctx)
    case Ast.ELet(binding, body) =>
      findInteractions(binding.bound, currentLoc, ctx) ++
        findInteractions(body, currentLoc, ctx)
    case Ast.ECase(scrut, alts) =>
      findInteractions(scrut, currentLoc, ctx) ++
        alts.toList.flatMap(a => findInteractions(a.expr, currentLoc, ctx))
    case Ast.ECons(_, front, tail) =>
      front.toList.flatMap(e2 => findInteractions(e2, currentLoc, ctx)) ++
        findInteractions(tail, currentLoc, ctx)

    // Inspect EVals to track function references.
    // NOTE: maybe eventually we need k-CFA or something?
    case Ast.EVal(qn) =>
      if (ctx.visited.contains(qn) || isStdlibPkgId(qn.packageId, ctx)) Nil
      else {
        val newCtx = ctx.copy(visited = ctx.visited + qn)
        (for {
          pkg  <- ctx.pkgsById.get(qn.packageId)
          mod  <- pkg.modules.get(qn.qualifiedName.module)
          dfn  <- mod.definitions.get(qn.qualifiedName.name)
        } yield dfn match {
          case dv: Ast.GenDValue[_] =>
            findInteractions(dv.body.asInstanceOf[Ast.Expr], currentLoc, newCtx)
          case _ => Nil
        }).getOrElse(Nil)
      }

    case _ => Nil
  }

  private def isStdlibPkgId(pkgId: PackageId, ctx: WalkCtx): Boolean =
    ctx.pkgsById.get(pkgId).exists { pkg =>
      val name = pkg.metadata.name.toString
      name.startsWith("daml-prim") ||
        name.startsWith("daml-stdlib") ||
        name.startsWith("ghc-stdlib") ||
        name.startsWith("ghc-prim")
    }

  private def findInteractionsInUpdate(
      u:          Ast.Update,
      currentLoc: Option[Ref.Location],
      ctx:        WalkCtx
  ): List[(Option[Ref.Location], Ast.Update)] = u match {
    case ex @ Ast.UpdateExercise(_, _, cidE, argE) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(cidE, currentLoc, ctx) ++
        findInteractions(argE, currentLoc, ctx)
    case ex @ Ast.UpdateExerciseInterface(_, _, cidE, argE, guard) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(cidE, currentLoc, ctx) ++
        findInteractions(argE, currentLoc, ctx) ++
        guard.toList.flatMap(e2 => findInteractions(e2, currentLoc, ctx))
    case ex @ Ast.UpdateExerciseByKey(_, _, key, argE) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(key, currentLoc, ctx) ++
        findInteractions(argE, currentLoc, ctx)
    case c @ Ast.UpdateCreate(_, expr) =>
      List((currentLoc, c: Ast.Update)) ++ findInteractions(expr, currentLoc, ctx)
    case c @ Ast.UpdateCreateInterface(_, expr) =>
      List((currentLoc, c: Ast.Update)) ++ findInteractions(expr, currentLoc, ctx)
    case f @ Ast.UpdateFetchTemplate(_, cid) =>
      List((currentLoc, f: Ast.Update)) ++ findInteractions(cid, currentLoc, ctx)
    case f @ Ast.UpdateFetchInterface(_, cid) =>
      List((currentLoc, f: Ast.Update)) ++ findInteractions(cid, currentLoc, ctx)
    case f @ Ast.UpdateFetchByKey(_)  => List((currentLoc, f: Ast.Update))
    case f @ Ast.UpdateLookupByKey(_) => List((currentLoc, f: Ast.Update))

    // just recursing for now WIP
    case Ast.UpdatePure(_, expr) => findInteractions(expr, currentLoc, ctx)
    case Ast.UpdateBlock(bindings, body) =>
      bindings.toList.flatMap(b => findInteractions(b.bound, currentLoc, ctx)) ++
        findInteractions(body, currentLoc, ctx)
    case Ast.UpdateEmbedExpr(_, body) => findInteractions(body, currentLoc, ctx)
    case Ast.UpdateLedgerTimeLT(expr) => findInteractions(expr, currentLoc, ctx)
    case Ast.UpdateTryCatch(_, body, _, catchE) =>
      findInteractions(body, currentLoc, ctx) ++
        findInteractions(catchE, currentLoc, ctx)

    case _ => Nil
  }
}
