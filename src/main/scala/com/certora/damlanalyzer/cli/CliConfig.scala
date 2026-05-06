package com.certora.damlanalyzer.cli

import scopt.OParser
import java.io.File

case class CliConfig(
    input: File = new File("."),
    format: String = "both",
    outputDir: Option[File] = None
)

object CliConfig {
  private val builder = OParser.builder[CliConfig]

  val parser: OParser[_, CliConfig] = {
    import builder._
    OParser.sequence(
      programName("daml-analyzer"),
      head("daml-analyzer", "0.1.0"),

      arg[File]("<dar-file-or-dir>")
        .required()
        .action((f, c) => c.copy(input = f))
        .text(
          "Path to a .dar file, OR a directory of .dar files"
        ),

      opt[String]('f', "format")
        .valueName("json|dot|both")
        .validate {
          case "json" | "dot" | "both" => Right(())
          case other                   =>
            Left(s"unknown format: $other (expected 'json', 'dot', or 'both')")
        }
        .action((s, c) => c.copy(format = s))
        .text(
          "Output format: 'both' (default when -o is set), 'json', or 'dot'. " +
            "Stdout only allows one of 'json' or 'dot'."
        ),

      opt[File]('o', "output")
        .valueName("<dir>")
        .action((f, c) => c.copy(outputDir = Some(f)))
        .text(
          "Output directory. This generates <name>.json and/or <name>.dot. " +
            "Required when input is a directory."
        ),

      help('h', "help").text("Print this message"),

      checkConfig { c =>
        if (c.input.isDirectory && c.outputDir.isEmpty)
          Left("batch mode requires -o <dir>")
        else if (c.outputDir.isEmpty && c.format == "both")
          Right(())
        else Right(())
      }
    )
  }

  def parse(args: Array[String]): Option[CliConfig] =
    OParser.parse(parser, args, CliConfig())
}
