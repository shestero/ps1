package podcasts

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.JdbcProfile
import sttp.capabilities.akka.AkkaStreams
import sttp.model.{Header, MediaType}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}


class PodcastsRouter(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext) extends PodcastsController {

  val updateAllEndpoint =
    endpoint.description(s"Update (download) all $prefix")
      .get
      .in(prefix / "update")
      .out(streamTextBody(AkkaStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
      .serverLogic[Future](logicUpdateAll)

  val updateEndpoint =
    endpoint.description(s"Update (download) $prefix with given id")
      .get
      .in(prefix / path[Long] / "update")
      .out(stringBody)
      .serverLogic(logicUpdateById)

  val itemsEndpoint =
    endpoint.description(s"All items of $prefix from given podcast id")
      .get
      .in(prefix / path[Long] / "items")
      .out(streamBinaryBody(AkkaStreams)(CodecFormat.Json()))
      .serverLogic(logicItemsByPodcastId)

  val channelsEndpoint =
    endpoint.description(s"All channels of $prefix from given podcast id")
      .get
      .in(prefix / path[Long] / "channels")
      .out(streamBinaryBody(AkkaStreams)(CodecFormat.Json()))
      .serverLogic(logicChannelsByPodcastId)

  val feedEndpoint =
    endpoint.description(s"Merged XML feed of a single $prefix")
      .get
      .in(prefix / path[Long] / "feed")
      .out(stringBody)
      .out(header(Header.contentType(MediaType.ApplicationXml)))
      .serverLogic(logicXMLById)

  override val endpoints: List[ServerEndpoint[AkkaStreams, Future]] =
    updateAllEndpoint +: getEndpoints :+ updateEndpoint :+ feedEndpoint :+ itemsEndpoint :+ channelsEndpoint
}

object PodcastsRouter {
  def apply()(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext):
    List[ServerEndpoint[AkkaStreams, Future]] =
      (new PodcastsRouter).endpoints
}
