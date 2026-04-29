lazy val root = (project in file("."))
  .settings(
    name         := "daml-analyzer",
    organization := "com.certora",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := "2.13.16",

    libraryDependencies ++= Seq(
      "com.daml"      %% "daml-lf-archive-reader" % "3.4.11",
      "org.slf4j"      % "slf4j-nop"              % "2.0.16",
      "org.scalatest" %% "scalatest"              % "3.2.19" % Test,
    ),
  )
