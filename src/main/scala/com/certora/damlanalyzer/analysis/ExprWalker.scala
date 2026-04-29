package com.certora.damlanalyzer.analysis

import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.daml.lf.language.Ast

object ExprWalker {
  // TODO: still missing some Expr variants like ERecCon, EStructCon, EVariantCon, EThrow, ETryCatch, and stuff
  def findInteractions(
      e: Ast.Expr,
      currentLoc: Option[Ref.Location] = None
  ): List[(Option[Ref.Location], Ast.Update)] = e match {

    case Ast.EUpdate(u) => findInteractionsInUpdate(u, currentLoc)

    case Ast.ELocation(loc, body) => findInteractions(body, Some(loc))

    case Ast.EAbs(_, body)     => findInteractions(body, currentLoc)
    case Ast.ETyAbs(_, body)   => findInteractions(body, currentLoc)
    case Ast.ETyApp(e2, _)     => findInteractions(e2, currentLoc)
    case Ast.ESome(_, body)    => findInteractions(body, currentLoc)
    case Ast.EToAny(_, body)   => findInteractions(body, currentLoc)
    case Ast.EFromAny(_, body) => findInteractions(body, currentLoc)

    case Ast.EApp(fn, arg) =>
      findInteractions(fn, currentLoc) ++ findInteractions(arg, currentLoc)
    case Ast.ELet(binding, body) =>
      findInteractions(binding.bound, currentLoc) ++ findInteractions(
        body,
        currentLoc
      )
    case Ast.ECase(scrut, alts) =>
      findInteractions(scrut, currentLoc) ++ alts.toList.flatMap(a =>
        findInteractions(a.expr, currentLoc)
      )
    case Ast.ECons(_, front, tail) =>
      front.toList.flatMap(e2 =>
        findInteractions(e2, currentLoc)
      ) ++ findInteractions(tail, currentLoc)

    case _ => Nil
  }

  private def findInteractionsInUpdate(
      u: Ast.Update,
      currentLoc: Option[Ref.Location]
  ): List[(Option[Ref.Location], Ast.Update)] = u match {
    case ex @ Ast.UpdateExercise(_, _, cidE, argE) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(cidE, currentLoc) ++ findInteractions(argE, currentLoc)
    case ex @ Ast.UpdateExerciseInterface(_, _, cidE, argE, guard) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(cidE, currentLoc) ++
        findInteractions(argE, currentLoc) ++
        guard.toList.flatMap(e2 => findInteractions(e2, currentLoc))
    case ex @ Ast.UpdateExerciseByKey(_, _, key, argE) =>
      List((currentLoc, ex: Ast.Update)) ++
        findInteractions(key, currentLoc) ++ findInteractions(argE, currentLoc)
    case c @ Ast.UpdateCreate(_, expr) =>
      List((currentLoc, c: Ast.Update)) ++ findInteractions(expr, currentLoc)
    case c @ Ast.UpdateCreateInterface(_, expr) =>
      List((currentLoc, c: Ast.Update)) ++ findInteractions(expr, currentLoc)
    case f @ Ast.UpdateFetchTemplate(_, cid) =>
      List((currentLoc, f: Ast.Update)) ++ findInteractions(cid, currentLoc)
    case f @ Ast.UpdateFetchInterface(_, cid) =>
      List((currentLoc, f: Ast.Update)) ++ findInteractions(cid, currentLoc)
    case f @ Ast.UpdateFetchByKey(_)  => List((currentLoc, f: Ast.Update))
    case f @ Ast.UpdateLookupByKey(_) => List((currentLoc, f: Ast.Update))

    // just recursing for now WIP
    case Ast.UpdatePure(_, expr)         => findInteractions(expr, currentLoc)
    case Ast.UpdateBlock(bindings, body) =>
      bindings.toList.flatMap(b => findInteractions(b.bound, currentLoc)) ++
        findInteractions(body, currentLoc)
    case Ast.UpdateEmbedExpr(_, body) => findInteractions(body, currentLoc)
    case Ast.UpdateLedgerTimeLT(expr) => findInteractions(expr, currentLoc)
    case Ast.UpdateTryCatch(_, body, _, catchE) =>
      findInteractions(body, currentLoc) ++ findInteractions(catchE, currentLoc)

    case _ => Nil
  }
}
