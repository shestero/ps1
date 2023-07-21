package util

import java.time.{OffsetDateTime, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.Try

object PodcastTime {
  val formats: LazyList[DateTimeFormatter] = LazyList(
    java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME,
    java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm[:ss] z"),
    java.time.format.DateTimeFormatter.ofPattern("EEE, d MMMM yyyy HH:mm[:ss] z")
  )
  // Note: unknown zone: PST, 'GM ';
  // Month cannot parse: NOV instead of Nov (?)

  private def parse(date: String)(format: DateTimeFormatter): Try[ZonedDateTime] =
    Try { ZonedDateTime.parse(date, format) }

  def parseRFC1123TimeKludgeOp(date: String): Option[ZonedDateTime] =
    formats
      .map(parse(date.trim))
      .find(_.isSuccess)
      .flatMap(_.toOption) // .map(_.get)

  def parseRFC1123TimeKludge(date: String): ZonedDateTime =
    parseRFC1123TimeKludgeOp(date).getOrElse {
      System.err.println(s"Cannot parse datetime value: '$date'")
      ZonedDateTime.now()
    }
}
