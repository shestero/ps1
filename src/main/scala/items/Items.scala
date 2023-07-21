package items

import slick.lifted.Tag
import util.PodcastTime

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.xml.Node
import scala.language.postfixOps

//import slick.jdbc.PostgresProfile.api._
import util.MyPostgresProfile.api._

class Items(tag: Tag) extends Table[Item](tag, "items") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def podcastId = column[Long]("podcast_id")
  def dt = column[OffsetDateTime]("dt")
  def pubDate = column[OffsetDateTime]("pubdate")
  def categories = column[List[String]]("categories")
  def xml = column[String]("item")

  def * = (id, podcastId, dt, pubDate, categories, xml) <>
    (Item.apply _ tupled, Item.unapply)
}
// ALTER TABLE items ADD CONSTRAINT uniq_items UNIQUE (podcast_id, item);
// https://stackoverflow.com/questions/30706193/insert-if-not-exists-in-slick-3-0-0

object Items {
  val placeholder = "<items/>"

  val table: TableQuery[Items] = TableQuery[Items]

  def insert(id: Long, podcastId: Long, xml: Node, categories: List[String]): DBIOAction[Int, NoStream, Effect.Write] = {
    val pubDate = PodcastTime.parseRFC1123TimeKludge((xml \\ "pubDate").text).toOffsetDateTime
    val xmls = xml.toString()
    table.forceInsertQuery {
      val exists = (for (e <- table if e.podcastId === podcastId.bind && e.xml === xmls.bind) yield e).exists
      val insert = (id, podcastId.bind, OffsetDateTime.now(), pubDate, categories, xmls.bind) <>
        (Item.apply _ tupled, Item.unapply)
      Query(insert).filterNot(_ => exists)
    }
  }

  def getByPodcastId(podcastId: Long): DBIO[Seq[(OffsetDateTime, String)]] =
    table.filter(_.podcastId===podcastId).sortBy(_.pubDate.desc).map(i => i.pubDate -> i.xml).result

  def getByPodcastIdStream(podcastId: Long) =
    table.filter(_.podcastId===podcastId).sortBy(_.pubDate.desc).result

  def byCategory(implicit ec: ExecutionContext): DBIOAction[Map[String, Set[Item]], NoStream, Effect.Read] = {
    table
      .map(r => r.categories -> r)
      .result
      .map(_.flatMap{ case (c, e) => c.map(_ -> Set(e)) }.groupMapReduce(_._1)(_._2)(_ ++ _))
  }
}
