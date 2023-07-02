package items

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.JdbcProfile
import sttp.tapir._
import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

class ItemsRouter(implicit profile: JdbcProfile, session: SlickSession) extends ItemsController {
  override val endpoints: List[ServerEndpoint[AkkaStreams, Future]] = getEndpoints
}

object ItemsRouter {
  def apply()(implicit profile: JdbcProfile, session: SlickSession): List[ServerEndpoint[AkkaStreams, Future]] =
    (new ItemsRouter).endpoints
}
