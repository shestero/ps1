package podcasts

import io.circe.generic.JsonCodec
import java.time.OffsetDateTime

@JsonCodec case class Podcast(id: Long, url: String, cntbrk: Int, lastd: Option[OffsetDateTime])
