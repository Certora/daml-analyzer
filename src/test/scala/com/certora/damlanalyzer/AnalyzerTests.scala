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
    val exerciseInterfaces = result.interactions.filter(_.interactionType == InteractionType.ExerciseInterface)
    assert(exerciseInterfaces.size == 1, s"expected 1 ExerciseInterface finding from Wallet.BurnToken, got ${exerciseInterfaces.size}")
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

  // Benchmark DARs from the official Daml SDK templates
  // these tests are not very strong yet, just checking basic things 

  test("test5: daml-sdk multi-package-example/main implements interface from other package") {
    val result  = loadAndAnalyze("/dars/mpx-test-main-1.0.0.dar")
    val finding = result.interactions
      .find(_.interactionType == InteractionType.ImplementsInterface)
      .getOrElse(fail("expected an ImplementsInterface finding"))
    assert(finding.target.pkg == "mpx-test-interfaces")
  }

  test("test6: daml-sdk upgrades-example main-v1 implements interface from other package") {
    val result  = loadAndAnalyze("/dars/upgrades-example-main-1.0.0.dar")
    val finding = result.interactions
      .find(_.interactionType == InteractionType.ImplementsInterface)
      .getOrElse(fail("expected an ImplementsInterface finding"))
    assert(finding.target.pkg == "upgrades-example-interfaces")
  }

  test("test7: daml-sdk upgrades-example main-v2 implements the same interface after SCU") {
    val result  = loadAndAnalyze("/dars/upgrades-example-main-2.0.0.dar")
    val finding = result.interactions
      .find(_.interactionType == InteractionType.ImplementsInterface)
      .getOrElse(fail("expected an ImplementsInterface finding"))
    assert(finding.target.pkg == "upgrades-example-interfaces")
  }

  // got from dpm template cache.
  // dpm new --list showed this as an available template
  test("test8: dpm daml-patterns is self-contained, 0  findings") {
    val result = loadAndAnalyze("/dars/daml-patterns-0.0.1.dar")
    assert(result.analyzedPackage.name == "daml-patterns")
    assert(result.summary.totalInteractions == 0, s"self-contained so expected zero findings but got ${result.summary.totalInteractions}")
  }

  // when an INTERFACE choice's body makes a cross-package call,
  // the caller must be reported with `interface` set and `template` empty
  // pkg-iasset's IAsset.Settle exercises Credit on Account in pkg-account.
  test("test9: cross package interface-choice caller is labeled with caller.interface, not caller.template") {
    val result = loadAndAnalyze("/dars/pkg-iasset-1.0.0.dar")
    assert(result.analyzedPackage.name == "pkg-iasset")

    val finding = result.interactions
      .find(i => i.interactionType == InteractionType.Exercise && i.target.template.contains("Account"))
      .getOrElse(fail("expected an Exercise finding targeting Account"))

    // interface-choice callers go in caller.interface, not caller.template.
    assert(finding.caller.interface.contains("IAsset"), s"caller.interface should be Some(\"IAsset\") but was ${finding.caller.interface}")
    assert(finding.caller.template.isEmpty, s"caller.template should be empty for an interface-choice caller but was ${finding.caller.template}")
    assert(finding.caller.choice.contains("Settle"))
    assert(finding.caller.module == "IAsset")

    // sanity checks
    assert(finding.target.pkg == "pkg-account")
    assert(finding.target.template.contains("Account"))
    assert(finding.target.choice.contains("Credit"))
    assert(finding.target.consuming.contains(true))
  }
}