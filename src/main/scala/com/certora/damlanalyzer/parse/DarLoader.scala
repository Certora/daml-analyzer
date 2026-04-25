package com.certora.damlanalyzer.parse

import com.digitalasset.daml.lf.archive.{Dar, DarDecoder}
import com.digitalasset.daml.lf.data.Ref.PackageId
import com.digitalasset.daml.lf.language.Ast
import java.io.File

object DarLoader {
  type LoadedDar = Dar[(PackageId, Ast.Package)]

  def loadDar(path: String): Either[String, LoadedDar] = {
    val file = new File(path)
    if (!file.exists())
      Left(s"File not found: ${file.getAbsolutePath}")
    else
      DarDecoder.readArchiveFromFile(file).left.map(_.toString)
  }
}
