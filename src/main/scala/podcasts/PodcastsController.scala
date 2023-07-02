package podcasts

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import scala.language.postfixOps
import Podcast._
import channels.Channels
import items.{Item, Items}
import items.Item._

import java.time.OffsetDateTime
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import routing.SourceConverters._
import sttp.capabilities.akka.AkkaStreams

abstract class PodcastsController(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext) extends routing.GetRouter[Podcast] {

  import profile.api._
  private val table: TableQuery[Podcasts] = TableQuery[Podcasts]
  private def queryId(id: Long) = table.filter(_.id === id)

  override def all: Source[Podcast, NotUsed] =
    Slick.source(
      table
        // .sortBy(_.id)
        .result
        .withStatementParameters(
          rsType = ResultSetType.ForwardOnly,
          rsConcurrency = ResultSetConcurrency.ReadOnly,
          fetchSize = 1000)
    )

  override def get(id: Long): Source[Podcast, NotUsed] =
    Slick.source(
      queryId(id)
        .result
        .withStatementParameters(
          rsType = ResultSetType.ForwardOnly,
          rsConcurrency = ResultSetConcurrency.ReadOnly,
          fetchSize = 1000)
    )

  val logicUpdateById: Long => Future[Either[Unit, String]] =
    util.Downloader.update(_).transform {
      case Success(s) => Success(s"The podcast updated:\n$s")
      case Failure(e) => Success(s"Update of the podcast failed: ${e.getMessage}")
    }.map(Right(_))

  val logicXMLById: Long => Future[Either[Unit, String]] = { podcastId =>
    val action = for {
      channelOp <- Channels.getByPodcastId(podcastId)
      channel = channelOp getOrElse s"<error>Channel for podcast with id=$podcastId not stored!</error>"
      items <- Items.getByPodcastId(podcastId)
    } yield channel.replace(
      Items.placeholder,
      items.sortBy(_._1)(Ordering[OffsetDateTime].reverse).map(_._2).mkString("\n")
    )
    session.db.run(action).map(Right.apply)
  }

  val logicItemsByPodcastId: Long => Future[Either[Unit, AkkaStreams.BinaryStream]] =
    Items.getByPodcastIdStream _ andThen Slick.source andThen toLogicJson[Item]
}
