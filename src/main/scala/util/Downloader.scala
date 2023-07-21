package util

import akka.stream.alpakka.slick.scaladsl.SlickSession
import channels.Channels
import items.Items
import podcasts.{Podcast, Podcasts}
//import sttp.client3.quick.backend
import sttp.client3.{HttpClientSyncBackend, asString, basicRequest}
import sttp.model.Uri

import java.time.OffsetDateTime
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.xml.XML.loadString
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}
import scala.util.chaining._
import cats.syntax.option._

import scala.concurrent.duration.DurationInt
import scala.util.Try

object Downloader {

  val timeout = 2.minutes

  private val removeItems = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
      case e: Elem if e.label == "items" => e.copy(child = NodeSeq.Empty)
      case n => n
    }
  }

  protected def node2categories(n: Node, root: Boolean = true): Set[String] =
    if (n.nonEmptyChildren.map(_.label)==Seq("#PCDATA"))
      Option(n.nonEmptyChildren.head.text.trim).filter(_.nonEmpty).toSet
    else
      n.nonEmptyChildren.filter(_.label.contains("category")).toSeq match {
        case Seq() => n.attributes.collectFirst{ case a if a.key=="text" => a.value.text }.toSet
        case children => children.collect {
          case c if c.label!="#PCDATA" =>
            node2categories(c, false).collect {
              case p if root => p.some
              case v => n.attributes.collectFirst{ case a if a.key=="text" => a.value.text + "/" + v }
            }.flatten
        }.flatten.toSet
      }

  type DownloadResult = Either[String, (String, Set[String], NodeSeq)]

  protected def download(url: String)(implicit ec: ExecutionContext): DownloadResult = // result: channel and items
    for {
      uri <- Uri.parse(url)
      tr = Try {
        val backend = HttpClientSyncBackend()
        Await.result(
          Future {
            basicRequest
              .get(uri)
              .response(asString)
              .readTimeout(timeout)
              .send(backend)
          },
          timeout + 5.second
        ).tap { _ =>
          backend.close()
        }
      }
      response <- tr.toEither.swap.map(s"Download $url error: " + _.getMessage).swap
      xml <- response.body

      // see also: https://dev64.wordpress.com/2012/04/05/replace-xml-part-using-regexp/
      begin = xml.indexOf("<item>")
      end = xml.lastIndexOf("</item>")
      channel = xml.substring(0, begin) + Items.placeholder + xml.substring(end + "</item>".length)
      rootNode = loadString(xml)
      categories = node2categories((rootNode \ "channel").head)
      _ = println(s"channel categories: $categories")
    } yield (channel, categories, (rootNode \\ "item"))

  def downloadPodcast(podcastId: Long)
                     (implicit session: SlickSession, ec: ExecutionContext): Future[(Podcast,DownloadResult)] =
  {
    //import profile.api._
    import slick.jdbc.PostgresProfile.api._

    println(s"update podcastId=$podcastId")

    val podcasts = TableQuery[Podcasts]
    val query = podcasts.filter(_.id === podcastId).take(1)
    val action = query.result.filter(_.nonEmpty).map(_.head)

    session.db.run(action).map { podcast =>
      println(s"podcast.url=${podcast.url}")
      podcast ->  download(podcast.url)
    }
  }

  def update(podcastId: Long)(implicit session: SlickSession, ec: ExecutionContext): Future[String] =
    downloadPodcast(podcastId).flatMap( { updateDownloaded(_, _) }.tupled )

  def updateDownloaded(podcast: Podcast, downloaded: DownloadResult)
                      (implicit session: SlickSession, ec: ExecutionContext): Future[String] =
  {
    val podcastId = podcast.id

    //import profile.api._
    import slick.jdbc.PostgresProfile.api._

    //println(s"update podcastId=$podcastId")

    val podcasts = TableQuery[Podcasts]

    downloaded match {
      case Left(error) =>
        println(s"Cannot download podcast with id=$podcastId: $error")
        val cntbrk = podcast.cntbrk + 1
        session.db.run(podcasts.insertOrUpdate(
          podcast.copy(cntbrk = cntbrk, lastd = OffsetDateTime.now().some)
        )).map(r => s"Cannot download podcast with id=$podcastId: $error.\n(cntbrk=$cntbrk update ret.code=$r)")
          .tap(println)

      case Right((channel, categories, items)) =>
        println(s"XML from podcastId=$podcastId parsed ok; ${items.size} items.")
        val actionChannel = Generator.next.flatMap { id =>
          Channels.insert(id, podcast.id, channel, categories)
        }
        val insertChannel = session.db.run(actionChannel)

        for {
          savedChannel <- insertChannel.tap{
            _.failed.foreach(e => println(s"Cannot insert a channel; error: ${e.getMessage}"))
          }
          _ = println(s"[podcast: ${podcast.id}]\tSaved $savedChannel channels")

          action = DBIO.sequence(
            items.reverse.map { itemNode => // from old to new
              val categories = node2categories(itemNode)
              if (categories.nonEmpty) println(s"item categories: $categories")
              for {
                id <- Generator.next
                insert <- Items.insert(id, podcast.id, itemNode, categories.toList)
              } yield insert
            }
          )
          savedItems <- session.db.run(action).tap{
            _.failed.foreach(e => println(s"Cannot insert items; error: ${e.getMessage}"))
          }
          _ = println(s"[podcast: ${podcast.id}]\tSaved ${savedItems.sum} items of ${items.size}")

          r <- session.db.run(podcasts.insertOrUpdate(
            podcast.copy(cntbrk = 0, lastd = OffsetDateTime.now().some)
          ))
          // _ = println("new obj saved " + podcast.copy(cntbrk = 0, lastd = OffsetDateTime.now().some) )
        } yield
          s"""Update podcastId=$podcastId (${podcast.url}):
             |\tSaved $savedChannel/1 channels.
             |\tSaved ${savedItems.sum}/${items.size} items.
             |\tNew podcast record saved (ret=$r)
             |""".stripMargin
    }
  }

}
