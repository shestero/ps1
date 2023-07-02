package routing

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.circe.Encoder
import sttp.tapir._
import sttp.capabilities.akka.AkkaStreams

//import java.nio.charset.StandardCharsets
import scala.concurrent.Future
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import SourceConverters._

abstract class GetRouter[T : TypeTag](implicit encoder: Encoder[T]) extends RouterInterface {

  def all: Source[T, NotUsed]
  def get(id: Long): Source[T, NotUsed]

  val prefix: String = typeOf[T].typeSymbol.name.toString.toLowerCase().replace("$", "")

  val logicAll: Unit => Future[Either[Unit, AkkaStreams.BinaryStream]] = _ => toLogicJson(all)
  val logicGetById: Long => Future[Either[Unit, AkkaStreams.BinaryStream]] = get _ andThen toLogicJson[T]

  val allEndpoint =
    endpoint.description(s"Get all $prefix")
      .get
      .in(prefix / "all")
      //.out(streamBody(AkkaStreams)(schema, CodecFormat.Json(), Some(StandardCharsets.UTF_8)))
      .out(streamBinaryBody(AkkaStreams)(CodecFormat.Json()))
      .serverLogic(logicAll)

  val getEndpoint =
    endpoint.description(s"Get $prefix with given id")
      .get
      .in(prefix / path[Long] )
      //.out(streamTextBody(AkkaStreams)(CodecFormat.TextPlain(), Some(StandardCharsets.UTF_8)))
      .out(streamBinaryBody(AkkaStreams)(CodecFormat.Json()))
      .serverLogic(logicGetById)

  protected val getEndpoints = allEndpoint :: getEndpoint :: Nil
}
