package util

import akka.stream.alpakka.slick.scaladsl.SlickSession
import channels.Channels
import items.Items
import podcasts.Podcasts
import sttp.client3.quick.backend
import sttp.client3.{Identity, Response, asString, basicRequest}
import sttp.model.Uri

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML.loadString
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}
import scala.util.chaining._
import cats.syntax.option._

object Downloader {

  private val removeItems = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
      case e: Elem if e.label == "items" => e.copy(child = NodeSeq.Empty)
      case n => n
    }
  }

  protected def download(url: String): Either[String, (String, NodeSeq)] = // result: channel and items
    for {
      uri <- Uri.parse(url)
      response: Identity[Response[Either[String, String]]] =
        basicRequest
          .get(uri)
          .response(asString)
          .send(backend)
      xml <- response.body

      // see also: https://dev64.wordpress.com/2012/04/05/replace-xml-part-using-regexp/
      begin = xml.indexOf("<item>")
      end = xml.lastIndexOf("</item>")
      channel = xml.substring(0, begin) + Items.placeholder + xml.substring(end + "</item>".length)
    } yield channel -> (loadString(xml) \\ "item")

  def update(podcastId: Long)(implicit session: SlickSession, ec: ExecutionContext): Future[String] = {
    //import profile.api._
    import slick.jdbc.PostgresProfile.api._

    println(s"update podcastId=$podcastId")

    val podcasts = TableQuery[Podcasts]
    val query = podcasts.filter(_.id === podcastId).take(1)
    val action = query.result.filter(_.nonEmpty).map(_.head)

    session.db.run(action).flatMap { podcast =>
      download(podcast.url) match {
        case Left(error) =>
          println(s"Cannot download podcast with id=$podcastId: $error")
          val cntbrk = podcast.cntbrk + 1
          session.db.run(podcasts.insertOrUpdate(
            podcast.copy(cntbrk = cntbrk, lastd = OffsetDateTime.now().some)
          )).map(r => s"Cannot download podcast with id=$podcastId: $error.\n(cntbrk=$cntbrk update ret.code=$r)")

        case Right((channel, items)) =>
          println(s"XML from podcastId=$podcastId parsed ok; ${items.size} items.")
          val actionChannel = Generator.next.flatMap { id =>
            Channels.insert(id, podcast.id, channel)
          }
          println("...")
          val insertChannel = session.db.run(actionChannel)

          for {
            savedChannel <- insertChannel.tap{
              _.failed.foreach(e => println(s"Cannot insert a channel; error: ${e.getMessage}"))
            }
            _ = println(s"[podcast: ${podcast.id}]\tSaved $savedChannel channels")

            action = DBIO.sequence(
              items.map(item => Generator.next -> item).map { case (idAction, xml) =>
                for {
                  id <- idAction
                  insert <- Items.insert(id, podcast.id, xml)
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
            s"""Saved $savedChannel/1 channels.
               |Saved ${savedItems.sum}/${items.size} items.
               |New podcast record saved (ret=$r)""".stripMargin
      }
    }
  }

}
