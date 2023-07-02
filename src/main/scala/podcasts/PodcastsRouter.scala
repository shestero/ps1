package podcasts

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.JdbcProfile
import sttp.capabilities.akka.AkkaStreams
import sttp.model.{Header, MediaType}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

import scala.concurrent.{ExecutionContext, Future}


class PodcastsRouter(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext) extends PodcastsController {

  val updateEndpoint =
    endpoint.description(s"Update (download) $prefix with given id")
      .get
      .in(prefix / path[Long] / "update")
      //.out(streamTextBody(AkkaStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
      .out(stringBody)
      .serverLogic(logicUpdateById)

  val itemsEndpoint =
    endpoint.description(s"Get items of $prefix with given id")
      .get
      .in(prefix / path[Long] / "items")
      //.out(streamTextBody(AkkaStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
      .out(streamBinaryBody(AkkaStreams)(CodecFormat.Json()))
      .serverLogic(logicItemsByPodcastId)

  val feedEndpoint =
    endpoint.description(s"Get merged XML feed of a single $prefix")
      .get
      .in(prefix / path[Long] / "feed")
      .out(stringBody)
      .out(header(Header.contentType(MediaType.ApplicationXml)))
      .serverLogic(logicXMLById)

  override val endpoints: List[ServerEndpoint[AkkaStreams, Future]] =
    getEndpoints :+ updateEndpoint :+ feedEndpoint :+ itemsEndpoint
}

object PodcastsRouter {
  def apply()(implicit profile: JdbcProfile, session: SlickSession, ec: ExecutionContext):
    List[ServerEndpoint[AkkaStreams, Future]] =
      (new PodcastsRouter).endpoints
}
