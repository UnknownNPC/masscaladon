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

There are two, and the second one defaults to the first:

| | Set with | Bounds | Fails with |
| --- | --- | --- | --- |
| **Client timeout** | `Masscaladon(..., timeout = ...)`, 60 s by default | every request the client sends, and how long `executeSync` waits | see below |
| **Request read timeout** | `.readTimeout(...)` on one request | that request only | `MasscaladonClientException` caused by sttp's `TimeoutException` |

A request's read timeout is how long it may take until the response starts
to arrive - sending the request (a whole upload, too) and the server's work
included. A request that doesn't set one gets the client timeout:

```scala
import scala.concurrent.duration.*

val mastodon = Masscaladon("https://example.social", token = "...", timeout = 10.minutes)

// Read timeout 10 minutes: the client's.
mastodon.executeSync(Requests.media.createMedia(file = File("big.mp4")))

// Read timeout 5 seconds: set on the request, so kept.
mastodon.executeSync(Requests.instance.getInstanceV2().readTimeout(5.seconds))
```

`executeSync` waits up to the client timeout, whatever the request's own
read timeout. Which one fires first decides the error:

```scala
val sync = Masscaladon("https://example.social", token = "...", timeout = 30.seconds)

// The request gives up first (10 s < 30 s):
//   Left(MasscaladonClientException("Unexpected failure", cause = sttp TimeoutException))
sync.executeSync(Requests.statuses.getStatus(id).readTimeout(10.seconds))

// executeSync gives up first (30 s < 2 min); the request goes on in the background:
//   Left(MasscaladonTimeoutException)
sync.executeSync(Requests.media.createMedia(file = File("big.mp4")).readTimeout(2.minutes))
```

So with `executeSync`, a per-request read timeout only matters when it is
shorter than the client's. With `execute` (a `Future`) only the read timeout
applies: there is nothing waiting on the `Future` but your own code.

The client timeout is also the connect timeout of the default backend. With
`Masscaladon(baseUrl, token, timeout, httpClient)`, connecting is up to the
`HttpClient` you pass (`HttpClient.newBuilder().connectTimeout(...)`); the
read timeout and the `executeSync` wait still apply.

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
