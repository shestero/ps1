package routing

import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import slick.jdbc.PostgresProfile.api._
import sttp.capabilities.akka.AkkaStreams
import podcasts.Podcasts
import channels.{Channel, Channels}
import items.{Item, Items}
import slick.dbio.DBIOAction
import util.Generator

import scala.concurrent.{ExecutionContext, Future}
import scala.util.chaining._

class CommonController {

  val logicSQLSchema: Unit => Future[Either[Unit, AkkaStreams.BinaryStream]] = _ => {
    val schemas = List(
      TableQuery[Podcasts].schema,
      TableQuery[Channels].schema,
      TableQuery[Items].schema,
      TableQuery[Generator].schema
    )
    val sql = schemas.flatMap(_.createIfNotExistsStatements).map(_.replace("\"","")+ ";\n")
    Future.successful( Right( Source( sql.map(ByteString(_)) ) ) )
  }

  def logicAllCategories(implicit ec: ExecutionContext, session: SlickSession):
    Unit => Future[Either[Unit, AkkaStreams.BinaryStream]] = _ => {

      val act: DBIOAction[Seq[String], NoStream, Effect.Read] = for {
        c <- Channels.byCategory
        //i <- Items.byCategory
        res = c
          .toSeq
          .sortBy(_._1)
          .collect{ case (k, mp) if k.nonEmpty =>
            mp
              .map{ case (podcastId, (c, p)) => (podcastId, c.title, c.topTag("link").getOrElse(p.url)) }
              .toSeq
              .sortBy(_._2)
              .map { case (podcastId, title, link) =>
                 s"""\t<li>[<a href="/podcast/$podcastId/feed">feed</a>] <a href="$link">$title</a></li>"""
              }
              .pipe(s"<h4>$k (${mp.size}):</h4>" +: "<ol>" +: _ :+ "</ol>")
            }
      } yield res.flatten

      session.db
        .run(act)
        .map(_.iterator.map(_ + "\n").map(ByteString(_)))
        .map(() => _)
        .map(Source.fromIterator)
        .map(Right[Unit, AkkaStreams.BinaryStream])
    }

}
