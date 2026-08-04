// build.sbt
import Settings.*
import Dependencies.*
import PublishSettings.*

import scala.sys.process.Process
import scala.sys.process.ProcessLogger

lazy val mastodonOpenApiJsonUrl =
  "https://raw.githubusercontent.com/abraham/mastodon-openapi/refs/heads/main/dist/schema.json"
lazy val mastodonOpenApiVersion = "4.5.0"
lazy val mastodonOpenApiSpecFileName =
  s"mastodon-openapi-$mastodonOpenApiVersion.json"

lazy val downloadMastodonSchema = taskKey[File](
  "Downloads the Mastodon OpenAPI spec file",
).withRank(KeyRanks.Invisible)

lazy val generateClient = taskKey[Unit](
  "Generates the Scala/circe/sttp client from the downloaded Mastodon OpenAPI spec",
).withRank(KeyRanks.Invisible)

lazy val openApiGeneratorConfig = settingKey[File](
  "Path to the openapi-generator config file",
).withRank(KeyRanks.Invisible)

lazy val OpenApiGeneratorTool = config("openApiGeneratorTool").hide

// Required for scalafix's semantic rules (e.g. RemoveUnused). Scala 3 emits
// semanticdb natively, no extra compiler plugin needed.
ThisBuild / semanticdbEnabled := true

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    name := "masscaladon",
  )
  .aggregate(openApiClient, scalaClient)

lazy val openApiClient = project.in(file("openapi-client"))
  .settings(commonSettings)
  // publishSettings is ThisBuild-scoped, so attaching it here makes it
  // apply build-wide — including to scalaClient's published POM. It's
  // attached to an actually-published module rather than root, so no
  // project ends up with both publishSettings and noPublishSettings.
  .settings(publishSettings)
  .settings(
    name := "masscaladon-openapi",
    organization := "com.github.unknownnpc",

    downloadMastodonSchema := {
      DownloadUtil.download(
        urlStr = mastodonOpenApiJsonUrl,
        fileName = mastodonOpenApiSpecFileName,
        destDir = baseDirectory.value / "schema",
        logger = streams.value.log,
      )
    },

    ivyConfigurations += OpenApiGeneratorTool,
    libraryDependencies +=
      ("org.openapitools" % "openapi-generator-cli" % openApiGeneratorVersion % OpenApiGeneratorTool)
        .intransitive(),

    openApiGeneratorConfig := baseDirectory.value / "openapi-config.yaml",

    // The stock scala-sttp templates predate Scala 3: they rely on circe's
    // blanket AutoDerivation (which chokes on recursive models like
    // Account.moved: Option[Account]) and build multipart bodies as a
    // Seq mixing bare and Option-wrapped elements (which Scala 3's stricter
    // `.flatten` inference rejects). templates/ overrides just those Mustache
    // partials so every model gets an explicit `derives Decoder, Encoder`
    // and multipart parts stay uniformly Option-wrapped.
    generateClient := {
      val log = streams.value.log
      val specFile = downloadMastodonSchema.value
      val generatorJar = Classpaths
        .managedJars(OpenApiGeneratorTool, Set("jar"), update.value)
        .map(_.data)
        .headOption
        .getOrElse(sys.error("openapi-generator-cli jar could not be resolved"))
      val outDir = baseDirectory.value
      val configFile = openApiGeneratorConfig.value
      val templateDir = baseDirectory.value / "templates"

      val fixedSpecFile = target.value / specFile.getName
      IO.write(fixedSpecFile, OneOfSpecFix.fix(IO.read(specFile)))

      val cmd = Seq(
        "java", "-jar", generatorJar.getAbsolutePath,
        "generate",
        "-g", "scala-sttp",
        "-i", fixedSpecFile.getAbsolutePath,
        "-o", outDir.getAbsolutePath,
        "-c", configFile.getAbsolutePath,
        "-t", templateDir.getAbsolutePath,
      )
      log.info(s"Running: ${cmd.mkString(" ")}")
      val exitCode = Process(cmd, outDir)
        .!(ProcessLogger(log.info(_), log.error(_)))
      if (exitCode != 0)
        sys.error(s"openapi-generator-cli failed with exit code $exitCode")
    },

    libraryDependencies ++= openApiRawClientDeps ++ scalaTest,
    publishTo := sonatypePublishToBundle.value,
  )

lazy val scalaClient = project.in(file("scala-client"))
  .settings(commonSettings)
  .settings(
    name := "masscaladon-client",
    organization := "com.github.unknownnpc",
    libraryDependencies ++= scalaClientDeps ++ scalaTest,
    // Enables the RemoveUnused scalafix rule; scoped to scala-client only
    // since openapi-client's generated sources always import codecs some
    // models don't need (see openapi-client/templates/model.mustache).
    scalacOptions += "-Wunused:all",
    publishTo := sonatypePublishToBundle.value,
  )
  .dependsOn(openApiClient)
