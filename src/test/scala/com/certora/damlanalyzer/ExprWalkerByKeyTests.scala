package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.ExprWalker
import com.digitalasset.daml.lf.data.{ImmArray, Ref}
import com.digitalasset.daml.lf.language.Ast
import org.scalatest.funsuite.AnyFunSuite


// It appears that `ExerciseByKey/FetchByKey/LookupByKey` are Daml-LF 1.x features.
// These tests construct the Update nodes directly in memory for testing.

class ExprWalkerByKeyTests extends AnyFunSuite {
  private val pkgId   = Ref.PackageId.assertFromString("a" * 64)
  private val qn      = Ref.QualifiedName.assertFromString("MyMod:MyTpl")
  private val tcid    = Ref.TypeConId(pkgId, qn)
  private val choice  = Ref.ChoiceName.assertFromString("MyChoice")

  private val placeholderExpr: Ast.Expr =
    Ast.EVar(Ref.Name.assertFromString("placeholder"))

  test("ExprWalker detects UpdateExerciseByKey") {
    val update = Ast.UpdateExerciseByKey(tcid, choice, placeholderExpr, placeholderExpr)
    val results = ExprWalker.findInteractions(Ast.EUpdate(update))
    assert(results.size == 1)
    assert(results.head._2 == update)
  }

  test("ExprWalker detects UpdateFetchByKey") {
    val update = Ast.UpdateFetchByKey(tcid)
    val results = ExprWalker.findInteractions(Ast.EUpdate(update))
    assert(results.size == 1)
    assert(results.head._2 == update)
  }

  test("ExprWalker detects UpdateLookupByKey") {
    val update = Ast.UpdateLookupByKey(tcid)
    val results = ExprWalker.findInteractions(Ast.EUpdate(update))
    assert(results.size == 1)
    assert(results.head._2 == update)
  }

  test("ExprWalker finds ByKey updates nested inside an UpdateBlock") {
    val ex = Ast.UpdateExerciseByKey(tcid, choice, placeholderExpr, placeholderExpr)
    val fb = Ast.UpdateFetchByKey(tcid)
    val lb = Ast.UpdateLookupByKey(tcid)

    val block = Ast.UpdateBlock(
      bindings = ImmArray(
        Ast.Binding(None, Ast.TBuiltin(Ast.BTUnit), Ast.EUpdate(ex)),
        Ast.Binding(None, Ast.TBuiltin(Ast.BTUnit), Ast.EUpdate(fb)),
        Ast.Binding(None, Ast.TBuiltin(Ast.BTUnit), Ast.EUpdate(lb)),
      ),
      body = Ast.EUpdate(Ast.UpdatePure(Ast.TBuiltin(Ast.BTUnit), placeholderExpr))
    )
    val results = ExprWalker.findInteractions(Ast.EUpdate(block))
    val updates = results.map(_._2).toSet
    assert(updates.contains(ex))
    assert(updates.contains(fb))
    assert(updates.contains(lb))
  }
}
