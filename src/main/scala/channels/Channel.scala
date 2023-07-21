package channels

import io.circe.generic.JsonCodec

import java.time.OffsetDateTime
import scala.xml.{Elem, XML}


@JsonCodec case class Channel(id: Long, podcastId: Long, dt: OffsetDateTime, categories: List[String], xml: String) {

  lazy val channel: Elem = XML.loadString(xml)

  def title: String =
    Option((channel \ "channel" \ "title").text)
      .map(_.trim)
      .filter(_.nonEmpty)
      //.orElse(Option((channel \\ "title").text).map(_.trim).filter(_.nonEmpty))
      .map(_.replaceAll("\\<.*?\\>", ""))
      .getOrElse("* Untitled *")
}
