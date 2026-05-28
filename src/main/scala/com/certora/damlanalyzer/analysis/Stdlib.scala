package com.certora.damlanalyzer.analysis

object Stdlib {
  // We filter libraries
  val Prefixes: Seq[String] = Seq("daml-prim", "daml-stdlib", "ghc-stdlib", "ghc-prim")

  def isStdlib(packageName: String): Boolean =
    Prefixes.exists(packageName.startsWith)
}
