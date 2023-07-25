package channels

import io.circe.generic.JsonCodec

import java.time.OffsetDateTime
import scala.xml.{Elem, XML}


@JsonCodec case class Channel(id: Long, podcastId: Long, dt: OffsetDateTime, categories: List[String], xml: String) {

  lazy val channel: Elem = XML.loadString(xml)

  def topTag(name: String): Option[String] =
    Option((channel \ "channel" \ name).text)
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(_.replaceAll("\\<.*?\\>", ""))

  def title: String = topTag("title") getOrElse "* Untitled *"
}
