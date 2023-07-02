package routing

import sttp.capabilities.akka.AkkaStreams
import sttp.tapir.server.ServerEndpoint
import scala.concurrent.Future

trait RouterInterface {

  val endpoints: List[ServerEndpoint[AkkaStreams, Future]]

}
