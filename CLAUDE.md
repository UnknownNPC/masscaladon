# masscaladon

A Scala 3 client for the Mastodon API. Two sbt modules, one pipeline:
download the spec → generate a client from it → wrap it in a hand-written,
idiomatic Scala 3 API.

## Design goal for `scala-client`

The point of the hand-written layer is a *pure, beautiful Scala 3 API*:
private constructors with companion-object factories, plain methods with
explicit arguments (no `given`/`using` ceremony in the public API — it was
tried and cut, see "The `scala-client` API" below), functional error
handling (`Either`, typed exceptions) instead of exceptions leaking across
API boundaries unchecked. Prefer that style over adding more boilerplate
classes when extending the client.

## Modules

- **`openapi-client`** — downloads the Mastodon OpenAPI spec
  (`downloadMastodonSchema` task; the fetched JSON is committed under
  `openapi-client/schema/` so a fresh checkout doesn't require network
  access to build) and generates the raw client from it (`generateClient`
  task). Generated sources under
  `openapi-client/src/main/scala/.../generated/` are also committed (vendored),
  not produced fresh on every build.
- **`scala-client`** — hand-written layer on top. See "The `scala-client` API"
  below.

## sbt tasks

```
sbt openApiClient/downloadMastodonSchema   # fetch openapi-client/schema/mastodon-openapi-<version>.json
sbt openApiClient/generateClient           # regenerate openapi-client/src/main/scala/.../generated
sbt compile                                # build everything
sbt test                                   # run tests (openapi-client has a JsonSupport smoke test)
sbt scalafmtAll                            # format all sources (excludes generated/ — see .scalafmt.conf)
sbt scalafmtCheckAll                       # verify formatting without changing files (CI)
sbt scalaClient/scalafixAll                # lint scala-client only (RemoveUnused; see .scalafix.conf)
sbt scalaClient/scalafixAll --check        # verify lint without changing files (CI)
sbt publishLocal                           # publish scala-client + openapi-client to the local Ivy repo
```

Re-run `generateClient` whenever the spec changes; it overwrites the
`generated` package in place. Nothing else in the pipeline runs
automatically — this is a deliberate two-step refresh, not a source
generator wired into `compile`.

## Git hooks

`hooks/pre-commit` runs `sbt scalafmtAll` on every commit and re-stages
whatever it reformats, so unformatted Scala never gets committed. It's
checked into the repo but git doesn't pick it up on its own — each clone
needs to opt in once with:

```
git config core.hooksPath hooks
```

## Publishing

Maven Central publishing uses two plugins (`project/plugins.sbt`): **sbt-pgp**
for signing (`publishSigned`, `publishLocalSigned`) and **sbt-sonatype** for
the Sonatype Central Portal upload/release (`sonatypePublishToBundle`,
`sonatypeBundleRelease`). `version` is set by hand in
`project/PublishSettings.scala` — we tried sbt-ci-release (git-tag-based
versions via sbt-dynver) on top, but its own `publishTo` logic actively
disagreed with sbt-sonatype's over where snapshots should go, so it was
dropped rather than papered over.

sbt-sonatype auto-reads `SONATYPE_USERNAME`/`SONATYPE_PASSWORD` from the
environment (no `credentials +=` needed). Sonatype sunset the legacy OSSRH
endpoint on 2025-06-30; publishing now goes through the Central Portal,
selected via `sonatypeCredentialHost := sonatypeCentralHost`. The groupId is
`com.github.unknownnpc` — a GitHub-verified namespace (see
`project/PublishSettings.scala`), not a custom-domain one, so no separate
domain-ownership proof is needed.

`publishLocal`/`publishM2`/`publishLocalSigned` don't touch Sonatype at all
and work without any credentials. With a non-SNAPSHOT `version`,
`publishSigned` also stays local — `sonatypePublishToBundle` resolves to a
`file:` resolver (`target/sonatype-staging/<version>/`) for releases, only
`sonatypeBundleRelease` actually uploads to Sonatype.

scalafix's semantic rules need semanticdb, enabled build-wide via
`ThisBuild / semanticdbEnabled := true` in `build.sbt` (Scala 3 emits it
natively, no extra compiler plugin). `RemoveUnused` is scoped to
`scala-client` only (`-Wunused:all` in that project's `scalacOptions`)
because `openapi-client`'s generated model files unconditionally import
codecs some models don't need — see `openapi-client/templates/model.mustache`.

## The `scala-client` API

Request *creation* and *execution* are fully decoupled:

- **Creation**: every generated `*Api` (`openapi-client/.../generated/api/`)
  is a plain `object` — no `baseUrl`, no client, nothing to construct. Its
  methods build an `sttp` `Request` with a relative URI (no host). `Requests`
  (`scala-client/.../client/Requests.scala`, hand-written) is every one of
  those objects in one place, so they're easy to find: `Requests.statuses`,
  `Requests.media`, `Requests.accounts`, etc.
- **Execution**: `Masscaladon` (`scala-client/.../client/Masscaladon.scala`)
  is a private-constructor class with a companion `apply` taking `baseUrl`
  and `token`. `.execute(request)`/`.executeSync(request)` resolve the
  request's relative URI against `baseUrl` (via `sttp.model.Uri.resolve`),
  authenticate, and send it:
  - **`.execute`** — async, returns `Future[T]`. Always fails with a
    `MasscaladonException` subtype (HTTP errors mapped by status code,
    network/decoding failures wrapped as `MasscaladonClientException`) —
    callers never need to catch an un-typed exception out of a failed
    `Future`.
  - **`.executeSync`** — blocking (for scripts, CLIs, synchronous frameworks
    that aren't set up for `Future`), returns `Either[MasscaladonException, T]`.
    Blocks up to the client's `timeout`; wraps `Await.result`'s own
    `TimeoutException` into `MasscaladonTimeoutException`. Exceptions are
    values here, not something the caller has to remember to catch.

This replaced an earlier design with one hand-written `*ApiWrapper` class per
API group (`MediaApiWrapper`, `StatusesApiWrapper`), each re-declaring every
generated method. That doesn't scale to 45 API groups and fights the "pure,
beautiful API" goal above.

Note for `generateClient`: `openapi-client/templates/api.mustache` builds
requests with a relative URI (`uri"{{{path}}}..."`, no `$baseUrl`) and no
longer takes a `baseUrl` constructor parameter — deliberate, not an
oversight. `Requests.scala` is *not* regenerated; when a new API tag
appears under `generated.api`, add its entry there by hand.

## Codegen: what's real and why it's customized

`openapi-client` shells out to the actual **OpenAPI Generator CLI**
(`org.openapitools:openapi-generator-cli`, generator `scala-sttp`), resolved
as a jar via sbt's own dependency resolution (see `OpenApiGeneratorTool`
config in `build.sbt`) — not Docker, not a manually-installed CLI. It's an
ordinary `libraryDependencies` entry in a hidden `ivyConfigurations` scope;
a fresh checkout resolves and caches it the same way it resolves circe or
sttp, no manual setup step.

An earlier attempt used Tapir's `sbt-openapi-codegen` plugin instead. It
could not parse the real spec at all (unresolved `$ref` response headers,
`text/event-stream` responses with no schema) and was dropped.

The stock `scala-sttp` templates predate Scala 3 and assume circe's blanket
`AutoDerivation`. That doesn't hold up here, so `openapi-client/templates/`
overrides a handful of the generator's own Mustache templates:

- **`model.mustache`** — every model gets an explicit Scala 3
  `derives Decoder, Encoder` instead of relying on `AutoDerivation`, which
  silently fails to derive recursive models (e.g. `Account.moved: Option[Account]`).
- **`jsonSupport.mustache`** — adds a `bodyOpt` extension instead of relying
  on `Encoder.encodeOption` for optional JSON bodies. `circeBodySerializer[Option[X]]`
  fails to resolve when `X`'s derived `Encoder` lives in a different file
  than the call site — a genuine Scala 3.8.1 cross-file derivation gap, not
  something fixable from the template alone. `bodyOpt` sidesteps it by
  encoding `X` directly and skipping the body on `None` (which is also more
  correct: a request with no body shouldn't serialize a literal `null`).
- **`paramMultipartCreation.mustache`** — required multipart parts are now
  wrapped in `Some(...)` so the `Seq(...).flatten` building a multipart body
  stays uniformly `Option`-typed; Scala 3 won't infer `.flatten` across a mix
  of bare and `Option`-wrapped elements the way Scala 2.13 did.
- **`dateSerializers.mustache` / `additionalTypeSerializers.mustache`** —
  turned from `trait`s mixed into `JsonSupport` into standalone `object`s
  with `given`s, so model files can import exactly the codecs they need
  (`URI`, `OffsetDateTime`, `LocalDate`) at the point where `derives` needs
  them in scope.

Other non-obvious pieces in `openapi-client/openapi-config.yaml`:

- `typeMappings: { object: io.circe.Json }` — free-form/untyped schema
  properties (e.g. `Rule.translations`) generate as `io.circe.Json` instead
  of `Any` (which circe can't derive a codec for). One header parameter
  (`Idempotency-Key` on `POST /api/v1/statuses`) is *also* typed as a
  free-form object in the upstream spec even though it's really a string;
  that's handled in the caller by wrapping a plain `String` in
  `Json.fromString` before passing it to `createStatus`, rather than fixing
  this generically.
- `-Xmax-inlines:64` in `project/Settings.scala` — circe's `derives`
  expansion for large models (`Status`, ~44 fields) exceeds the default
  inline budget (32).

If codegen ever needs to change, edit `openapi-client/templates/*.mustache`
or `openapi-client/openapi-config.yaml`, then re-run `generateClient` — don't
hand-edit files under `generated/`, they get overwritten.
