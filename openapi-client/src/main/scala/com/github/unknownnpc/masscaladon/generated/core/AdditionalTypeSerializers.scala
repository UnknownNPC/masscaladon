package com.github.unknownnpc.masscaladon.generated.core

import java.net.{ URI, URISyntaxException }

object AdditionalTypeSerializers {
    import io.circe._

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
