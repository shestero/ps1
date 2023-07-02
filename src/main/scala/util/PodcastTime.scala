package util

import scala.util.Try

object PodcastTime {
  val RFC1123_KLUDGY =
    java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm[:ss] z")

  def getRFC1123Time(date: String): java.time.ZonedDateTime =
    java.time.ZonedDateTime.parse(date, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)

  def optRFC1123Time(date: String): Option[java.time.ZonedDateTime] =
    Try(getRFC1123Time(date)).toOption

  def getRFC1123TimeKludge(date: String): java.time.ZonedDateTime = {
    optRFC1123Time(date).getOrElse(
      java.time.ZonedDateTime.parse(date, RFC1123_KLUDGY)
    )
  }
}
