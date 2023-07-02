package podcasts

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._
import java.time.OffsetDateTime
import scala.language.postfixOps

class Podcasts(tag: Tag) extends Table[Podcast](tag, "podcasts") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def url = column[String]("url")
  def cntbrk = column[Int]("cntbrk")
  def lastd = column[Option[OffsetDateTime]]("lastd")

  def * = (id, url, cntbrk, lastd) <> (Podcast.apply _ tupled, Podcast.unapply)
}
