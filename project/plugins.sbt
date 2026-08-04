addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.7")
// Signing (sbt-pgp) + Sonatype Central Portal upload/release (sbt-sonatype).
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.2")

libraryDependencies += "io.circe" %% "circe-parser" % "0.14.10"
