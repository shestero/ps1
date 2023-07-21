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
        k = c.keySet.filter(_.nonEmpty) //++ i.keySet
        f <-
          DBIOAction.sequence(
            k
              .map(k => k -> c.getOrElse(k, Set.empty)) // (c.getOrElse(k, Set.empty), i.getOrElse(k, Set.empty)))
              .toSeq // .toMap
              .sortBy(_._1)
              .collect{ case (k, cs) if cs.nonEmpty =>
                DBIOAction
                  .sequence(
                    cs
                      .map(c => (c.podcastId, c.title))
                      .toSeq
                      .sortBy(_._2)
                      .map { case (podcastId, title) =>
                        podcastId
                          .pipe(Podcasts.queryId)
                          .map(_.url)
                          .result
                          .headOption
                          .map(_.map(url =>
                            s"""<li>[<a href="/podcast/$podcastId/feed">feed</a>] <a href="$url">$title</a></li>""")
                          )
                      }
                  )
                  .map(s"<h4>$k (${cs.size}):</h4>" +: "<ol>" +: _.flatMap(_.map("\t" + _)) :+ "</ol>")
              }
          )
          .map(_.flatten)
      } yield f.map(_ + "\n")

      session.db
        .run(act)
        .map(_.iterator.map(ByteString(_)))
        .map(() => _)
        .map(Source.fromIterator)
        .map(Right[Unit, AkkaStreams.BinaryStream])
    }

}
