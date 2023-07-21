package routing

import akka.stream.alpakka.slick.scaladsl.SlickSession
import slick.jdbc.JdbcProfile
import sttp.capabilities.akka.AkkaStreams
import sttp.model.{HeaderNames, StatusCode}
import sttp.tapir.files.staticFilesGetServerEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{CodecFormat, endpoint, header, statusCode, streamTextBody}

import java.nio.charset.StandardCharsets
import scala.concurrent.{ExecutionContext, Future}

class CommonRouter(implicit ec: ExecutionContext, session: SlickSession) extends CommonController with RouterInterface {

  val redirectOutput = statusCode(StatusCode.PermanentRedirect).and(header[String](HeaderNames.Location))

  val logicRoot: Unit => Future[Either[Unit, String]] = _ => Future.successful( Right( "/static" ) )

  val rootEndpoint =
    endpoint.description("Root endpoint")
      .get.in("")
      .out(redirectOutput)
      .serverLogic(logicRoot)

  val schemaEndpoint =
    endpoint.description("View CREATE TABLE SQL")
      .get.in("sql")
      .out(streamTextBody(AkkaStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
      .serverLogic(logicSQLSchema)

  val catAllEndpoint =
    endpoint.description("View all categories")
      .get.in("cat")
      .out(streamTextBody(AkkaStreams)(CodecFormat.TextHtml(), Some(StandardCharsets.UTF_8)))
      .serverLogic(logicAllCategories)

  val staticEndpoint = staticFilesGetServerEndpoint[Future]("static")("static")

  override val endpoints: List[ServerEndpoint[AkkaStreams, Future]] =
    rootEndpoint :: schemaEndpoint :: catAllEndpoint :: staticEndpoint :: Nil
}

object CommonRouter {
  def apply()(implicit profile: JdbcProfile, ec: ExecutionContext, session: SlickSession):
    List[ServerEndpoint[AkkaStreams, Future]] =
      (new CommonRouter).endpoints
}
