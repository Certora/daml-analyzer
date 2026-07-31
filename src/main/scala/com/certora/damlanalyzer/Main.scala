package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.cli.CliConfig
import com.certora.damlanalyzer.output.{DotReporter, HtmlReporter, JsonReporter}
import com.certora.damlanalyzer.parse.DarLoader
import com.certora.damlanalyzer.schema.AnalysisResult
import java.io.{File, PrintWriter}
import scala.collection.mutable

object Main {
  def main(args: Array[String]): Unit = {
    val config = CliConfig.parse(args).getOrElse(sys.exit(1))

    val dars: List[File] =
      if (config.input.isDirectory) {
        config.input.listFiles().toList.filter(_.getName.endsWith(".dar")).sorted
      }
      else {
        List(config.input)
      }

    if (dars.isEmpty) {
      System.err.println(s"no .dar files found in ${config.input.getAbsolutePath}")
      sys.exit(1)
    }

    config.outputDir.foreach { dir =>
      if (!dir.exists()) {
        dir.mkdirs()
      }
      if (!dir.isDirectory) {
        System.err.println(s"-o must be a directory: ${dir.getAbsolutePath}")
        sys.exit(1)
      }
    }

    val fmts: Set[String] = config.format match {
      case "json" => Set("json")
      case "dot"  => Set("dot")
      case "html" => Set("html")
      case "both" => Set("json", "dot")
      case "all"  => Set("json", "dot", "html")
    }

    var failed = 0
    val results = mutable.ListBuffer[AnalysisResult]()
    dars.foreach { dar =>
      DarLoader.loadDar(dar.getAbsolutePath) match {
        case Right(loaded) =>
          val result = CrossPackageAnalyzer.analyze(loaded)
          val stem   = dar.getName.stripSuffix(".dar")
          config.outputDir match {
            case Some(dir) =>
              if (fmts("json")) writeFile(new File(dir, s"$stem.json"), JsonReporter.render(result))
              if (fmts("dot"))  writeFile(new File(dir, s"$stem.dot"),  DotReporter.render(result))
              if (fmts("html") && dars.size == 1)
                writeFile(new File(dir, s"$stem.html"), HtmlReporter.render(Seq(result)))
              results += result
            case None =>
              val out = config.format match {
                case "dot"  => DotReporter.render(result)
                case "html" => HtmlReporter.render(Seq(result))
                case _      => JsonReporter.render(result)
              }
              println(out)
          }
        case Left(err) =>
          System.err.println(s"failed to load ${dar.getName}: $err")
          failed += 1
      }
    }

    config.outputDir.foreach { dir =>
      if (fmts("html") && dars.size > 1 && results.nonEmpty) {
        writeFile(new File(dir, "report.html"), HtmlReporter.render(results.toSeq))
      }
    }

    if (failed > 0) sys.exit(1)
  }

  private def writeFile(f: File, content: String): Unit = {
    val w = new PrintWriter(f)
    try {
      w.write(content)
    }
    finally {
      w.close()
    }
  }
}
