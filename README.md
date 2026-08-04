# masscaladon

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

`Masscaladon` is purely a request *executor* — it holds your auth token and
`baseUrl` and runs requests, nothing more. Requests are built independently,
with no client and no host attached, via `Requests` (every generated `*Api`
object in one place):

```scala
import scala.concurrent.ExecutionContext

import com.github.unknownnpc.masscaladon.client.{Masscaladon, Requests}
import com.github.unknownnpc.masscaladon.generated.model.CreateStatusRequest

given ExecutionContext = ExecutionContext.global
val mastodon = Masscaladon("https://example.social", token = "...")

// async
val request = Requests.statuses.createStatus(CreateStatusRequest(status = "hello"))
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
import com.github.unknownnpc.masscaladon.generated.model.UpdateStatusRequestPoll

val post =
  for
    media <- mastodon.executeSync(Requests.media.createMedia(file = File("cat.jpg")))
    status <- mastodon.executeSync(Requests.statuses.createStatus(
      CreateStatusRequest(
        status = "look at this cat",
        mediaIds = Seq(media.id),
        poll = UpdateStatusRequestPoll(),
      ),
    ))
  yield status

post match
  case Right(status) => println(s"posted: ${status.id}")
  case Left(error)   => println(s"failed: ${error.getMessage}")
```

## Project layout

Two sbt modules, one pipeline: download the spec and generate a raw client
from it → wrap it in the hand-written `scala-client` API shown above.

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
