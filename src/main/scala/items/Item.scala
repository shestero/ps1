package items

import io.circe.generic.JsonCodec
import java.time.OffsetDateTime

@JsonCodec case class Item(
                            id: Long,
                            podcastId: Long,
                            dt: OffsetDateTime,
                            pubDate: OffsetDateTime,
                            categories: List[String],
                            xml: String
                          )

