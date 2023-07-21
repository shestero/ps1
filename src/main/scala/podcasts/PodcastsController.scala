package podcasts

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import scala.language.postfixOps
import Podcast._
import akka.util.ByteString
import channels.Channels
import items.{Item, Items}
import items.Item._
import channels.{Channel, Channels}

import java.time.OffsetDateTime
import routing.SourceConverters._
import sttp.capabilities.akka.AkkaStreams
import util.Downloader

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration.DurationInt

import podcasts.Podcasts._


abstract class PodcastsController(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext) extends routing.GetRouter[Podcast] {

  val parallelism = 16

  import profile.api._

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

  val transformUpdateResult: Future[String] => Future[Either[Unit, String]] =
    _.transform {
      case Success(s) => Success(s"The podcast updated:\n$s")
      case Failure(e) => Success(s"Update of the podcast failed: ${e.getMessage}")
    }.map(Right(_))

  val logicUpdateById: Long => Future[Either[Unit, String]] =
    { Downloader.update(_) } andThen transformUpdateResult

  def updateAll: AkkaStreams.BinaryStream =
    Slick
      .source(table.sortBy(_.lastd.desc.nullsFirst).map(_.id).result)
      // .map(logicUpdateById).flatMapConcat(Source.future)
      .mapAsyncUnordered(parallelism)(
        Downloader.downloadPodcast(_)
          .transform { case t: Try[(Podcast, Downloader.DownloadResult)] =>
            t.map(Right(_)).recover { e => Left(s"Error downloading: ${e.getMessage}") }
          }
      )
      .map(_.fold(Future.successful, { Downloader.updateDownloaded(_,_) }.tupled))
      .flatMapConcat(Source.future)
      .map(_ + "\n")
      .map(ByteString(_))
      // .map(_.map(_ + "\n").fold(_ => ByteString.empty, ByteString(_)))

  val logicUpdateAll: Unit => Future[Either[Unit, AkkaStreams.BinaryStream]] =
    _ => toLogicPlainText(updateAll.keepAlive(15.seconds, () => ByteString("...")))

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

  val logicChannelsByPodcastId: Long => Future[Either[Unit, AkkaStreams.BinaryStream]] =
    Channels.getByPodcastIdStream _ andThen Slick.source andThen toLogicJson[Channel]
}
