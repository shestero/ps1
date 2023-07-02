package channels

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.JdbcProfile
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future

class ChannelsRouter(implicit profile: JdbcProfile, session: SlickSession) extends ChannelsController {
  override val endpoints: List[ServerEndpoint[AkkaStreams, Future]] = getEndpoints
}

object ChannelsRouter {
  def apply()(implicit profile: JdbcProfile, session: SlickSession): List[ServerEndpoint[AkkaStreams, Future]] =
    (new ChannelsRouter).endpoints
}
