package com.github.unknownnpc.masscaladon.generated.core

import java.net.{ URI, URISyntaxException }

object AdditionalTypeSerializers {
    import io.circe._

    // Mastodon's JSON is snake_case; every model's field names are the camelCase Scala
    // convention. Wildcard-imported (`AdditionalTypeSerializers.given`) into every generated
    // model alongside the other type serializers, so this one Configuration drives every
    // model's ConfiguredEncoder/ConfiguredDecoder derivation (see model.mustache) without
    // per-model wiring.
    given jsonNamingConfiguration: io.circe.derivation.Configuration =
      io.circe.derivation.Configuration.default.withSnakeCaseMemberNames

    given URIDecoder: Decoder[URI] = Decoder.decodeString.emap(string =>
      try Right(new URI(string))
      catch {
        case _: URISyntaxException =>
          Left("String could not be parsed as a URI reference, it violates RFC 2396.")
        case _: NullPointerException =>
          Left("String is null.")
      }
    )

    given URIEncoder: Encoder[URI] = new Encoder[URI] {
      final def apply(a: URI): Json = Json.fromString(a.toString)
  }
}
