import sbt.*

object Dependencies {

  val scala3Version = "3.8.1"
  val sttpVersion = "3.11.0"
  val circeVersion = "0.14.10"
  val scalaTestVersion = "3.2.19"
  val openApiGeneratorVersion = "7.10.0"

  lazy val openApiRawClientDeps: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe" % sttpVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
  )

  lazy val scalaClientDeps: Seq[ModuleID] =
    Seq(
      "org.slf4j" % "slf4j-api" % "2.0.12",
      "ch.qos.logback" % "logback-classic" % "1.5.3",
    )

  lazy val scalaTest: Seq[ModuleID] =
    Seq("org.scalatest" %% "scalatest" % scalaTestVersion % Test)
}
