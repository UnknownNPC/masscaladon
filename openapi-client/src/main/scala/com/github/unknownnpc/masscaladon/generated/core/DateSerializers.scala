package com.github.unknownnpc.masscaladon.generated.core

import java.time.{LocalDate, OffsetDateTime}
import java.time.format.DateTimeFormatter

object DateSerializers {
    import io.circe.{Decoder, Encoder}
    given isoOffsetDateTimeDecoder: Decoder[OffsetDateTime] = Decoder.decodeOffsetDateTimeWithFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    given isoOffsetDateTimeEncoder: Encoder[OffsetDateTime] = Encoder.encodeOffsetDateTimeWithFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    given localDateDecoder: Decoder[LocalDate] = Decoder.decodeLocalDateWithFormatter(DateTimeFormatter.ISO_LOCAL_DATE)
    given localDateEncoder: Encoder[LocalDate] = Encoder.encodeLocalDateWithFormatter(DateTimeFormatter.ISO_LOCAL_DATE)
}
