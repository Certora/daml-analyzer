package com.certora.damlanalyzer.analysis

import com.digitalasset.daml.lf.language.Ast

object ExprWalker {
  // TODO: do more
  // starting to travese the AST
  // finds UpdateExercise nodes reachable from an "Expr".
  def findExercises(e: Ast.Expr): List[Ast.UpdateExercise] = e match {
    case Ast.EUpdate(ex: Ast.UpdateExercise) => List(ex)
    case Ast.EAbs(_, body)                   => findExercises(body)
    case _                                   => Nil
  }
}
