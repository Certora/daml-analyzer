package com.certora.damlanalyzer.cli

import scopt.OParser
import java.io.File

case class CliConfig(
  darFile: File         = new File("."),
  format:  String       = "json", // "json" or "dot"
  output:  Option[File] = None
)

object CliConfig {
  private val builder = OParser.builder[CliConfig]

  val parser: OParser[_, CliConfig] = {
    import builder._
    OParser.sequence(
      programName("daml-analyzer"),
      head("daml-analyzer", "0.1.0"),

      arg[File]("<dar-file>")
        .required()
        .action((f, c) => c.copy(darFile = f))
        .text("Path to the .dar file to analyze"),

      opt[String]('f', "format")
        .valueName("json|dot")
        .validate {
          case "json" | "dot" => Right(())
          case other          => Left(s"unknown format: $other (expected 'json' or 'dot')")
        }
        .action((s, c) => c.copy(format = s))
        .text("Output format, default json"),

      opt[File]('o', "output")
        .valueName("<path>")
        .action((f, c) => c.copy(output = Some(f)))
        .text("Output file, default: stdout"),

      help('h', "help").text("Print this usage message"),
    )
  }

  def parse(args: Array[String]): Option[CliConfig] =
    OParser.parse(parser, args, CliConfig())
}
