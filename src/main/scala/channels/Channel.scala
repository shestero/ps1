package channels

import io.circe.generic.JsonCodec

//import java.sql.Timestamp
import java.time.OffsetDateTime

@JsonCodec case class Channel(id: Long, podcastId: Long, dt: OffsetDateTime, xml: String)
