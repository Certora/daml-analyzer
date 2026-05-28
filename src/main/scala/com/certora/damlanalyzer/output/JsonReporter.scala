package com.certora.damlanalyzer.output

import com.certora.damlanalyzer.schema._
import io.circe.{Encoder, Json, Printer}
import io.circe.syntax._

// Renders an AnalysisResult as JSON.
object JsonReporter {

  private val printer = Printer.spaces2.copy(dropNullValues = true, colonLeft = "")

  def render(result: AnalysisResult): String = printer.print(result.asJson)

  private implicit val packageRefEncoder: Encoder[PackageRef] =
    Encoder.instance { d =>
      Json.obj(
        "name" -> d.name.asJson,
        "version" -> d.version.asJson,
        "packageId" -> d.packageId.asJson
      )
    }

  private implicit val interactionTypeEncoder: Encoder[InteractionType] =
    Encoder.encodeString.contramap(_.toString)

  private implicit val analyzedPackageEncoder: Encoder[AnalyzedPackage] =
    Encoder.instance { p =>
      Json.obj(
        "name" -> p.name.asJson,
        "version" -> p.version.asJson,
        "packageId" -> p.packageId.asJson,
        "lfVersion" -> p.lfVersion.asJson
      )
    }

  private implicit val sourceLocationEncoder: Encoder[SourceLocation] =
    Encoder.instance { s =>
      Json.obj(
        "file" -> s.file.asJson,
        "startLine" -> s.startLine.asJson,
        "startColumn" -> s.startColumn.asJson,
        "endLine" -> s.endLine.asJson,
        "endColumn" -> s.endColumn.asJson
      )
    }

  private implicit val callerEncoder: Encoder[Caller] =
    Encoder.instance { c =>
      Json.obj(
        "package" -> c.pkg.asJson,
        "version" -> c.version.asJson,
        "packageId" -> c.packageId.asJson,
        "module" -> c.module.asJson,
        "template" -> c.template.asJson,
        "interface" -> c.interface.asJson,
        "choice" -> c.choice.asJson
      )
    }

  private implicit val targetEncoder: Encoder[Target] =
    Encoder.instance { t =>
      Json.obj(
        "package" -> t.pkg.asJson,
        "version" -> t.version.asJson,
        "packageId" -> t.packageId.asJson,
        "module" -> t.module.asJson,
        "template" -> t.template.asJson,
        "interface" -> t.interface.asJson,
        "choice" -> t.choice.asJson,
        "consuming" -> t.consuming.asJson
      )
    }

  private implicit val interactionEncoder: Encoder[CrossPackageInteraction] =
    Encoder.instance { i =>
      Json.obj(
        "type" -> i.interactionType.asJson,
        "source" -> i.source.asJson,
        "caller" -> i.caller.asJson,
        "target" -> i.target.asJson
      )
    }

  private implicit val summaryEncoder: Encoder[Summary] =
    Encoder.instance { s =>
      Json.obj(
        "totalInteractions" -> s.totalInteractions.asJson,
        "byType" -> s.byType.asJson,
        "byTargetPackage" -> s.byTargetPackage.asJson
      )
    }

  private implicit val resultEncoder: Encoder[AnalysisResult] =
    Encoder.instance { r =>
      Json.obj(
        "analyzedPackage" -> r.analyzedPackage.asJson,
        "dependencies" -> r.dependencies.asJson,
        "summary" -> r.summary.asJson,
        "interactions" -> r.interactions.asJson
      )
    }
}
