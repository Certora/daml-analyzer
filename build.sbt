lazy val root = (project in file("."))
  .settings(
    name := "daml-analyzer",
    organization := "com.certora",
    version := "0.1.2",
    scalaVersion := "2.13.16",

    libraryDependencies ++= Seq(
      "com.daml" %% "daml-lf-archive" % "3.5.6",
      "org.slf4j" % "slf4j-nop" % "2.0.16",
      "com.github.scopt" %% "scopt" % "4.1.0",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),

    // Using larger JVM stack to avoid stack overflow.
    run  / fork        := true,
    Test / fork        := true,
    run  / javaOptions += "-Xss4m",
    Test / javaOptions += "-Xss4m",

    // `sbt assembly` configs, may need to fix later
    Compile / mainClass        := Some("com.certora.damlanalyzer.Main"),
    assembly / assemblyJarName := s"daml-analyzer-${version.value}.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*)             => MergeStrategy.discard
      case other =>
        val default = (assembly / assemblyMergeStrategy).value
        default(other)
    }
  )
