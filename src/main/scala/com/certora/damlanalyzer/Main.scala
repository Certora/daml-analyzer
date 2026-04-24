package com.certora.damlanalyzer

import com.digitalasset.daml.lf.archive.DarDecoder
import java.io.File

object Main {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      System.err.println("Usage: daml-analyzer <dar-file>")
      sys.exit(1)
    }

    val darFile = new File(args(0))
    if (!darFile.exists()) {
      System.err.println(s"File not found: ${darFile.getAbsolutePath}")
      sys.exit(1)
    }

    println(s"Loading DAR: ${darFile.getName}")

    DarDecoder.readArchiveFromFile(darFile) match {
      case Right(dar) =>
        val (mainPkgId, mainPkg) = dar.main
        val mainMeta = mainPkg.metadata
        println(s"Main package: ${mainMeta.name} ${mainMeta.version}")
        println(s"Main package ID: $mainPkgId")
        println(s"Total packages in DAR: ${dar.all.size}")
        println("All packages:")
        dar.all.foreach { case (pkgId, pkg) =>
          val m = pkg.metadata
          println(f"  ${pkgId.toString}%-18s ${m.name} ${m.version}")
        }
      case Left(err) =>
        System.err.println(s"Failed to decode DAR: $err")
        sys.exit(1)
    }
  }
}
