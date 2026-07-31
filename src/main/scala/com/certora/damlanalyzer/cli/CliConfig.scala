package com.certora.damlanalyzer.cli

import scopt.OParser
import java.io.File

case class CliConfig(
    input: File = new File("."),
    format: String = "all", // default when -o is set: json + dot + html
    outputDir: Option[File] = None
)

object CliConfig {
  private val builder = OParser.builder[CliConfig]

  val parser: OParser[_, CliConfig] = {
    import builder._

    OParser.sequence(
      programName("daml-analyzer"),
      head("daml-analyzer", "0.1.2"),

      arg[File]("<dar-file-or-dir>")
        .required()
        .action((f, c) => c.copy(input = f))
        .text(
          "Path to a .dar file, OR a directory of .dar files"
        ),

      opt[String]('f', "format")
        .valueName("json|dot|html|all")
        .validate {
          case "json" | "dot" | "html" | "all" | "both" => Right(())
          case other => Left(s"unknown format: $other (expected 'json', 'dot', 'html', or 'all')")
        }
        .action((s, c) => c.copy(format = s))
        .text("Output format: 'all' (json+dot+html, default when -o is set), 'json', 'dot', or 'html'. Stdout only allows one of 'json', 'dot', or 'html'."),

      opt[File]('o', "output")
        .valueName("<dir>")
        .action((f, c) => c.copy(outputDir = Some(f)))
        .text("Output directory. This generates <name>.json and/or <name>.dot. Required when input is a directory."),

      help('h', "help").text("Print this message"),

      checkConfig { c =>
        if (c.input.isDirectory && c.outputDir.isEmpty) {
          Left("batch mode requires -o <dir>")
        } else if (c.outputDir.isEmpty && (c.format == "both" || c.format == "all")) {
          Left("stdout dump must be run with -f json, -f dot, or -f html")
        } else {
          Right(())
        }
      }
    )
  }

  def parse(args: Array[String]): Option[CliConfig] =
    OParser.parse(parser, args, CliConfig())
}
