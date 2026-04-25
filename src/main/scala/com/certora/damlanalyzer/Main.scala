package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.output.JsonReporter
import com.certora.damlanalyzer.parse.DarLoader

object Main {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      System.err.println("Usage: daml-analyzer <dar-file>")
      sys.exit(1)
    }

    DarLoader.loadDar(args(0)) match {
      case Right(dar) =>
        val result = CrossPackageAnalyzer.analyze(dar)
        println(JsonReporter.render(result))
      case Left(err) =>
        System.err.println(err)
        sys.exit(1)
    }
  }
}
