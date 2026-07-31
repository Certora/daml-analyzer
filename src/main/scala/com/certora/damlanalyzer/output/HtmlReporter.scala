package com.certora.damlanalyzer.output

import com.certora.damlanalyzer.schema.AnalysisResult
import scala.io.Source

object HtmlReporter {

  private lazy val template: String = {
    val src = Source.fromResource("viewer/index.html")
    try src.mkString
    finally src.close()
  }

  def render(results: Seq[AnalysisResult]): String = {
    val safeJson = JsonReporter.renderArrayCompact(results).replace("<", "\\u003c")
    val inject   = s"""<script>window.EMBEDDED_RESULTS=$safeJson;</script>"""
    val idx      = template.indexOf("<script>")
    require(idx >= 0, "viewer template missing <script> tag")
    template.substring(0, idx) + inject + "\n" + template.substring(idx)
  }
}
