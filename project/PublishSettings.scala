import sbt.*
import sbt.Keys.*
import xerial.sbt.Sonatype.autoImport.sonatypeCredentialHost
import xerial.sbt.Sonatype.sonatypeCentralHost

object PublishSettings {

  private val githubUrl = "https://github.com/UnknownNPC/masscaladon"

  // ThisBuild-scoped so this applies build-wide no matter which project
  // attaches it. Bump `version` by hand for each release.
  lazy val publishSettings: Seq[Setting[?]] = Seq(
    ThisBuild / version := "0.0.1",
    ThisBuild / versionScheme := Some("early-semver"),
    ThisBuild / homepage := Some(url(githubUrl)),
    ThisBuild / licenses := Seq(
      "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"),
    ),
    ThisBuild / scmInfo := Some(
      ScmInfo(url(githubUrl), "scm:git@github.com:UnknownNPC/masscaladon.git"),
    ),
    ThisBuild / developers := List(
      Developer(
        id = "UnknownNPC",
        name = "Vitalii Zymukha",
        email = "unknownvzzv@gmail.com",
        url = url("https://github.com/UnknownNPC"),
      ),
    ),
    ThisBuild / sonatypeCredentialHost := sonatypeCentralHost,
  )

  lazy val noPublishSettings: Seq[Setting[?]] = Seq(
    publish / skip := true,
  )
}
