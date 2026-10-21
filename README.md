# masscaladon

[![Maven Central](https://img.shields.io/maven-central/v/com.github.unknownnpc/masscaladon-client_3.svg)](https://central.sonatype.com/artifact/com.github.unknownnpc/masscaladon-client_3/overview)

A Scala 3 client for the [Mastodon](https://joinmastodon.org/) API, built on
[sttp](https://sttp.softwaremill.com/) and [circe](https://circe.github.io/circe/),
with an idiomatic Scala 3 surface: `derives`-based type classes for every
request/response model, request creation and execution fully decoupled,
private constructors with companion-object factories, and functional error
handling instead of unchecked exceptions.

> **Note:** This project — the build pipeline, the OpenAPI codegen
> customization, and the client API — was largely put together via vibe
> coding with [Claude Code](https://claude.com/claude-code). Review it before
> relying on it for anything production-critical.

## Getting started

Requirements: JDK 17+, [sbt](https://www.scala-sbt.org/) 1.12+.

```sh
sbt compile   # builds both modules; spec + generated client are already committed
sbt test
```

```scala
import scala.concurrent.ExecutionContext

import com.github.unknownnpc.masscaladon.client.{Masscaladon, Requests}
import com.github.unknownnpc.masscaladon.generated.model.CreateStatusRequest

given ExecutionContext = ExecutionContext.global
val mastodon = Masscaladon("https://example.social", token = "...")

// async
val request = Requests.statuses.createStatus(CreateStatusRequest(status = Some("hello")))
mastodon.execute(request)

// blocking, e.g. from a script
mastodon.executeSync(Requests.statuses.getStatus(id)) match
  case Right(status) => println(status)
  case Left(error)   => println(s"failed: ${error.getMessage}")
```

Posting media works the same way: upload it first, then reference the
returned attachment's `id` in the status:

```scala
import java.io.File
import scala.concurrent.duration.*

// Any one request may take up to `timeout` (60 seconds by default): give
// video uploads more.
val uploader = Masscaladon("https://example.social", token = "...", timeout = 10.minutes)

val post =
  for
    media <- uploader.executeSync(Requests.media.createMedia(file = File("cat.mp4")))
    status <- uploader.executeSync(Requests.statuses.createStatus(
      CreateStatusRequest(
        status = Some("look at this cat"),
        mediaIds = Some(Seq(media.id)),
      ),
      idempotencyKey = Some(s"cat-${media.id}"),
    ))
  yield status

post match
  case Right(status) => println(s"posted: ${status.id}")
  case Left(error)   => println(s"failed: ${error.getMessage}")
```

`idempotencyKey` makes a retry safe: for an hour, Mastodon answers a key it
has seen with the status it created first instead of posting again. So the
key must be unique to the post - never a constant.

## Timeouts

**Most of the time you set one number: the client's `timeout`.** It is how
long any single request may take, and it defaults to 60 seconds:

```scala
import scala.concurrent.duration.*

// Fine for posting text, reading timelines, etc.
val mastodon = Masscaladon("https://example.social", token = "...")

// Uploading videos? Allow each request more time.
val uploader = Masscaladon("https://example.social", token = "...", timeout = 10.minutes)
```

"Take" means from sending the request until the response starts to arrive,
so it covers the whole upload of a file plus the server's processing. If a
large upload fails with a timeout, raise this number.

### A different timeout for one request

Give a single request its own limit with `.readTimeout(...)`. It wins over
the client's `timeout` for that request only:

```scala
// Every other request gets 10 minutes; this one gives up after 5 seconds.
uploader.executeSync(Requests.instance.getInstanceV2().readTimeout(5.seconds))
```

With `executeSync`, keep a per-request limit *below* the client's `timeout`:
`executeSync` never waits longer than the client's `timeout`, so a longer
per-request limit has no effect there. (`execute` returns a `Future` and
doesn't wait at all, so a request's own limit always applies.)

### Telling a timeout apart from other errors

```scala
import com.github.unknownnpc.masscaladon.exception.*
import sttp.client3.SttpClientException

uploader.executeSync(Requests.media.createMedia(file = File("big.mp4"))) match
  case Right(media) => println(s"uploaded: ${media.id}")
  // The request ran out of its time (the client's `timeout`, or its own `.readTimeout`).
  case Left(e: MasscaladonClientException) if e.getCause.isInstanceOf[SttpClientException.TimeoutException] =>
    println("upload timed out")
  // executeSync stopped waiting (the client's `timeout`); the request may still finish.
  case Left(e: MasscaladonTimeoutException) => println("gave up waiting")
  case Left(e) => println(s"failed: ${e.getMessage}")
```

Connecting to the server is limited by the same `timeout`. If you pass your
own `java.net.http.HttpClient` to `Masscaladon(baseUrl, token, timeout, httpClient)`,
connecting is up to that client instead
(`HttpClient.newBuilder().connectTimeout(...)`); everything above still applies.

## Project layout

Two sbt modules, one pipeline: download the spec, generate a raw client from
it, wrap it in the hand-written `scala-client` API shown above.

```
sbt openApiClient/downloadMastodonSchema   # fetch the OpenAPI spec
sbt openApiClient/generateClient           # regenerate the raw client from it
sbt compile                                # build everything
sbt test                                   # run tests
sbt scalafmtAll                            # format all sources
sbt scalafmtCheckAll                       # verify formatting (CI)
sbt scalaClient/scalafixAll                # lint scala-client
sbt scalaClient/scalafixAll --check        # verify lint (CI)
sbt publishLocal                           # publish to the local Ivy repo
```

See [`CLAUDE.md`](./CLAUDE.md) for the full architecture: why the codegen
pipeline is customized the way it is, and the design of the `scala-client`
API layer.
