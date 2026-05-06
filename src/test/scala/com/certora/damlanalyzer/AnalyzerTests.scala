package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.parse.DarLoader
import com.certora.damlanalyzer.schema._
import org.scalatest.funsuite.AnyFunSuite

class AnalyzerTests extends AnyFunSuite {

  private def loadAndAnalyze(resourcePath: String): AnalysisResult = {
    val url  = getClass.getResource(resourcePath)
    assert(url != null, s"resource not found on test classpath: $resourcePath")
    val path = url.getFile
    DarLoader.loadDar(path) match {
      case Right(dar) => CrossPackageAnalyzer.analyze(dar)
      case Left(err)  => fail(s"failed to load $resourcePath: $err")
    }
  }

  test("test1: pkg-vault produces 1 cross-package Exercise finding") {
    val result = loadAndAnalyze("/dars/pkg-vault-1.0.0.dar")

    assert(result.analyzedPackage.name == "pkg-vault")
    assert(result.summary.totalInteractions == 1)
    assert(result.summary.byType == Map("Exercise" -> 1))

    val finding = result.interactions.head
    assert(finding.interactionType == InteractionType.Exercise)
    assert(finding.target.pkg == "pkg-asset")
    assert(finding.target.module == "Asset")
    assert(finding.target.template.contains("Asset"))
    assert(finding.target.choice.contains("Transfer"))
    assert(finding.target.consuming.contains(true))
  }


  test("test2: pkg-impl produces an ImplementsInterface finding for MyToken on IToken") {
    val result = loadAndAnalyze("/dars/pkg-impl-1.0.0.dar")

    assert(result.analyzedPackage.name == "pkg-impl")
    val finding = result.interactions
      .find(_.interactionType == InteractionType.ImplementsInterface)
      .getOrElse(fail("no ImplementsInterface finding produced"))
    assert(finding.caller.template.contains("MyToken"))
    assert(finding.target.pkg == "pkg-interface")
    assert(finding.target.module == "IToken")
    assert(finding.target.interface.contains("IToken"))
    assert(finding.target.template.isEmpty)
    assert(finding.target.choice.isEmpty)
    assert(finding.target.consuming.isEmpty)
  }

  test("test3: pkg-impl Wallet.BurnToken produces an ExerciseInterface finding (call-graph)") {
    val result = loadAndAnalyze("/dars/pkg-impl-1.0.0.dar")
    val exerciseInterfaces =
      result.interactions.filter(_.interactionType == InteractionType.ExerciseInterface)
    assert(exerciseInterfaces.size == 1,
      s"expected 1 ExerciseInterface finding from Wallet.BurnToken, got ${exerciseInterfaces.size}")
    val finding = exerciseInterfaces.head
    assert(finding.target.pkg == "pkg-interface")
    assert(finding.target.interface.contains("IToken"))
    assert(finding.target.choice.contains("Burn"))
    // IToken.Burn is a default `choice` so consuming is true
    assert(finding.target.consuming.contains(true))
  }

  test("test4: pkg-app produces 5 cross-package findings (Create, Fetch, Exercise, FetchInterface, ImplementsInterface)") {
    val result = loadAndAnalyze("/dars/pkg-app-1.0.0.dar")

    assert(result.analyzedPackage.name == "pkg-app")
    assert(result.summary.totalInteractions == 5)
    assert(result.summary.byType == Map(
      "Create"              -> 1,
      "Fetch"               -> 1,
      "Exercise"            -> 1,
      "FetchInterface"      -> 1,
      "ImplementsInterface" -> 1,
    ))

    def findingOf(t: InteractionType): CrossPackageInteraction =
      result.interactions.find(_.interactionType == t)
        .getOrElse(fail(s"missing finding of type $t"))

    val exercise = findingOf(InteractionType.Exercise)
    assert(exercise.target.pkg == "pkg-registry")
    assert(exercise.target.module == "Registry")
    assert(exercise.target.template.contains("Asset"))
    assert(exercise.target.choice.contains("UpdateOwner"))
    assert(exercise.target.consuming.contains(true))

    val fetch = findingOf(InteractionType.Fetch)
    assert(fetch.target.pkg == "pkg-registry")
    assert(fetch.target.template.contains("Asset"))
    assert(fetch.target.choice.isEmpty)

    val create = findingOf(InteractionType.Create)
    assert(create.target.pkg == "pkg-registry")
    assert(create.target.template.contains("Asset"))

    val fetchInterface = findingOf(InteractionType.FetchInterface)
    assert(fetchInterface.target.pkg == "pkg-iclaim")
    assert(fetchInterface.target.interface.contains("IClaim"))
    assert(fetchInterface.target.template.isEmpty)

    val implementsInterface = findingOf(InteractionType.ImplementsInterface)
    assert(implementsInterface.caller.template.contains("MyToken"))
    assert(implementsInterface.target.pkg == "pkg-iclaim")
    assert(implementsInterface.target.interface.contains("IClaim"))
  }
}
