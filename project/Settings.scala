import sbt.*
import sbt.Keys.*

object Settings {
  import Dependencies.scala3Version

  lazy val commonSettings = Seq(
    scalaVersion := scala3Version, // 3.8.1
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      // circe's derives-based Decoder/Encoder derivation expands one inline
      // per field; Mastodon's larger entities (e.g. Status) exceed the
      // default inline budget (32).
      "-Xmax-inlines:64",
    ),
  )
}
