package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.ExprWalker
import com.digitalasset.daml.lf.data.Ref
import com.digitalasset.daml.lf.language.Ast
import org.scalatest.funsuite.AnyFunSuite


class ExprWalkerSyntheticTests extends AnyFunSuite {

  private val pkgId   = Ref.PackageId.assertFromString("a" * 64)
  private val modName = Ref.DottedName.assertFromString("MyMod")
  private val tplName = Ref.DottedName.assertFromString("MyTpl")
  private val tcid    = Ref.TypeConId(pkgId, Ref.QualifiedName(modName, tplName))

  private val placeholderExpr: Ast.Expr =
    Ast.EVar(Ref.Name.assertFromString("placeholder"))

  test("ExprWalker detects UpdateFetchTemplate") {
    val update  = Ast.UpdateFetchTemplate(tcid, placeholderExpr)
    val results = ExprWalker.findInteractions(Ast.EUpdate(update))
    assert(results.size == 1)
    assert(results.head._2 == update)
  }
}
