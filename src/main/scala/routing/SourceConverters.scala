package routing

import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.circe.Encoder
import io.circe.syntax._
import sttp.capabilities.akka.AkkaStreams

import scala.concurrent.Future
import scala.util.Try


object SourceConverters {

  def toLogicJson[T](source: Source[T, Any])(implicit encoder: Encoder[T]) =
    Future.successful( Try{
      val separators = Source.single("") ++ Source.repeat(",\n")
      val strings = (separators zipWith source){ case (sep, t) => sep + t.asJson.noSpaces }
      (Source.single("[\n") ++ strings ++ Source.single("\n]")).map(ByteString(_))
    }.toEither.swap.map{ e => println(s"Error: ${e.getMessage}") }.swap )

  def toLogicPlainText(source: AkkaStreams.BinaryStream) =
    Future.successful( Right(source) )

}
