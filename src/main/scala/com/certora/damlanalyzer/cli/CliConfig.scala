package com.certora.damlanalyzer.cli

import com.certora.damlanalyzer.BuildInfo
import com.certora.damlanalyzer.cli.OutputFormat.All
import scopt.OParser
import java.io.File

case class CliConfig(
    input: File = new File("."),
    format: OutputFormat = All, // default when -o is set: json + dot + html
    outputDir: Option[File] = None
)

object CliConfig {
  private val builder = OParser.builder[CliConfig]

  private val stdoutHint    = OutputFormat.singles.map(f => s"-f ${f.name}").mkString(", ")
  private val singlesJoined = OutputFormat.singles.map(_.name).mkString("+")
  private val singlesQuoted = OutputFormat.quoteJoined(OutputFormat.singles)

  val parser: OParser[_, CliConfig] = {
    import builder._

    OParser.sequence(
      programName(BuildInfo.name),
      head(BuildInfo.name, BuildInfo.version),

      arg[File]("<dar-file-or-dir>")
        .required()
        .action((f, c) => c.copy(input = f))
        .text("Path to a .dar file, OR a directory of .dar files"),

      opt[OutputFormat]('f', "format")
        .valueName(OutputFormat.displayList)
        .action((s, c) => c.copy(format = s))
        .text(s"Output format: '${All.name}' ($singlesJoined, default when -o is set), $singlesQuoted. Stdout only allows one of $singlesQuoted."),

      opt[File]('o', "output")
        .valueName("<dir>")
        .action((f, c) => c.copy(outputDir = Some(f)))
        .text("Output directory. Required when input is a directory."),

      help('h', "help").text("Print this message"),

      checkConfig { c =>
        if (c.input.isDirectory && c.outputDir.isEmpty) {
          Left("batch mode requires -o <dir>")
        } else if (c.outputDir.isEmpty && c.format == All) {
          Left(s"stdout dump must be run with $stdoutHint")
        } else {
          Right(())
        }
      }
    )
  }

  def parse(args: Array[String]): Option[CliConfig] =
    OParser.parse(parser, args, CliConfig())
}
