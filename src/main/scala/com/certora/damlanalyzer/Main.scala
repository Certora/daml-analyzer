package com.certora.damlanalyzer

import com.certora.damlanalyzer.analysis.CrossPackageAnalyzer
import com.certora.damlanalyzer.cli.CliConfig
import com.certora.damlanalyzer.output.{DotReporter, JsonReporter}
import com.certora.damlanalyzer.parse.DarLoader
import java.io.{File, PrintWriter}

object Main {
  def main(args: Array[String]): Unit = {
    val config = CliConfig.parse(args).getOrElse(sys.exit(1))

    val dars: List[File] =
      if (config.input.isDirectory)
        config.input.listFiles().toList.filter(_.getName.endsWith(".dar")).sorted
      else
        List(config.input)

    if (dars.isEmpty) {
      System.err.println(s"no .dar files found in ${config.input.getAbsolutePath}")
      sys.exit(1)
    }

    config.outputDir.foreach { dir =>
      if (!dir.exists()) dir.mkdirs()
      if (!dir.isDirectory) {
        System.err.println(s"-o must be a directory: ${dir.getAbsolutePath}")
        sys.exit(1)
      }
    }

    var failed = 0
    dars.foreach { dar =>
      DarLoader.loadDar(dar.getAbsolutePath) match {
        case Right(loaded) =>
          val result = CrossPackageAnalyzer.analyze(loaded)
          val stem   = dar.getName.stripSuffix(".dar")
          config.outputDir match {
            case Some(dir) =>
              if (config.format == "both" || config.format == "json")
                writeFile(new File(dir, s"$stem.json"), JsonReporter.render(result))
              if (config.format == "both" || config.format == "dot")
                writeFile(new File(dir, s"$stem.dot"), DotReporter.render(result))
            case None =>
              val out = config.format match {
                case "dot" => DotReporter.render(result)
                case _     => JsonReporter.render(result)
              }
              println(out)
          }
        case Left(err) =>
          System.err.println(s"failed to load ${dar.getName}: $err")
          failed += 1
      }
    }
    if (failed > 0) sys.exit(1)
  }

  private def writeFile(f: File, content: String): Unit = {
    val w = new PrintWriter(f)
    try w.write(content)
    finally w.close()
  }
}
