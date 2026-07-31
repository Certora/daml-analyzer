package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.output.{HtmlReporter, JsonReporter}
import com.certora.damlanalyzer.parse.DarLoader
import com.certora.damlanalyzer.schema._
import io.circe.Json
import org.scalatest.funsuite.AnyFunSuite

class HtmlReporterTests extends AnyFunSuite {

  private def analyze(resourcePath: String): AnalysisResult = {
    val path = getClass.getResource(resourcePath).getFile
    DarLoader.loadDar(path) match {
      case Right(dar) => CrossPackageAnalyzer.analyze(dar)
      case Left(err)  => fail(s"loadDar failed: $err")
    }
  }

  private def parseOrFail(s: String, label: String): Json =
    io.circe.parser.parse(s).fold(e => fail(s"$label not parseable: $e"), j => j)

  private val EmbedPattern = """(?s)<script>window\.EMBEDDED_RESULTS=(.+?);</script>""".r

  test("HtmlReporter output contains the DOCTYPE, embeds JSON, and round-trips vs JsonReporter") {
    val result = analyze("/dars/pkg-app-1.0.0.dar")
    val html   = HtmlReporter.render(Seq(result))

    assert(html.startsWith("<!DOCTYPE html>"), "output must be a full HTML document")

    val embedded = EmbedPattern.findFirstMatchIn(html).getOrElse(fail("no EMBEDDED_RESULTS script tag found")).group(1)
    val embArr   = parseOrFail(embedded, "embedded JSON")
    val embOne   = embArr.asArray.getOrElse(fail("embed must be a JSON array")).headOption.getOrElse(fail("embed array empty"))

    val generated = parseOrFail(JsonReporter.render(result), "generated JSON")
    assert(embOne == generated, "embedded JSON must be the same as generated JSON so the viewer renders the same view")
  }

  test("HtmlReporter aggregates multiple results into one embedded array") {
    val r1   = analyze("/dars/pkg-app-1.0.0.dar")
    val r2   = analyze("/dars/pkg-impl-1.0.0.dar")
    val html = HtmlReporter.render(Seq(r1, r2))

    val embedded = EmbedPattern.findFirstMatchIn(html).getOrElse(fail("no embed")).group(1)
    val arr      = parseOrFail(embedded, "embedded JSON").asArray.getOrElse(fail("not array"))
    assert(arr.size == 2, s"expected 2-element array, got ${arr.size}")
  }
}
