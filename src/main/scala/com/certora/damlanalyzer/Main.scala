package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.cli.CliConfig
import com.certora.damlanalyzer.output.{DotReporter, JsonReporter}
import com.certora.damlanalyzer.parse.DarLoader
import java.io.PrintWriter

object Main {
  def main(args: Array[String]): Unit = {
    val config = CliConfig.parse(args).getOrElse(sys.exit(1))

    DarLoader.loadDar(config.darFile.getAbsolutePath) match {
      case Right(dar) =>
        val result = CrossPackageAnalyzer.analyze(dar)
        val output = config.format match {
          case "json" => JsonReporter.render(result)
          case "dot"  => DotReporter.render(result)
        }
        config.output match {
          case Some(file) =>
            val writer = new PrintWriter(file)
            try writer.write(output)
            finally writer.close()
          case None =>
            println(output)
        }
      case Left(err) =>
        System.err.println(err)
        sys.exit(1)
    }
  }
}
