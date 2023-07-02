package channels

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import java.time.OffsetDateTime
import scala.language.postfixOps

class Channels(tag: Tag) extends Table[Channel](tag, "channels") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def podcastId = column[Long]("podcast_id")
  def dt = column[OffsetDateTime]("dt")
  def xml = column[String]("channel")

  def * = (id, podcastId, dt, xml) <> (Channel.apply _ tupled, Channel.unapply)
}
// ALTER TABLE channels ADD CONSTRAINT uniq_channel UNIQUE (podcast_id, channel);
// https://stackoverflow.com/questions/30706193/insert-if-not-exists-in-slick-3-0-0

object Channels {
  val table: TableQuery[Channels] = TableQuery[Channels]

  def insert(id: Long, podcastId: Long, xml: String): DBIOAction[Int, NoStream, Effect.Write] =
    table.forceInsertQuery {
      val exists = (for (e <- table if e.podcastId === podcastId.bind && e.xml === xml.bind) yield e).exists
      val insert = (id.bind, podcastId.bind, OffsetDateTime.now(), xml.bind) <> (Channel.apply _ tupled, Channel.unapply)
      Query(insert).filterNot(_ => exists)
    }

  def getByPodcastId(podcastId: Long): DBIO[Option[String]] =
    table.filter(_.podcastId===podcastId).sortBy(_.dt.desc.nullsLast).map(_.xml).take(1).result.headOption
}
