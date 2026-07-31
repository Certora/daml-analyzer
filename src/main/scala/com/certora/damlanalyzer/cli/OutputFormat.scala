package com.certora.damlanalyzer.cli

sealed trait OutputFormat {
  def name: String
  def emits(f: OutputFormat.Single): Boolean
}

object OutputFormat {

  sealed trait Single extends OutputFormat { final def emits(f: Single): Boolean = f == this }
  case object Json extends Single { val name = "json" }
  case object Dot  extends Single { val name = "dot"  }
  case object Html extends Single { val name = "html" }

  case object All extends OutputFormat {
    val name = "all"
    def emits(f: Single): Boolean = true
  }

  val singles:     List[Single]       = List(Json, Dot, Html)
  val values:      List[OutputFormat] = singles :+ All
  val displayList: String             = values.map(_.name).mkString("|")

  def quoteJoined(xs: List[OutputFormat]): String =
    xs.map(v => s"'${v.name}'").mkString(", ")

  private val byName = values.map(v => v.name -> v).toMap

  def parse(s: String): Either[String, OutputFormat] =
    byName.get(s).toRight(s"unknown format: $s (expected ${quoteJoined(values)})")

  implicit val scoptRead: scopt.Read[OutputFormat] =
    scopt.Read.reads(s => parse(s).fold(m => throw new IllegalArgumentException(m), identity))
}
