package routing

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.circe.Encoder
import io.circe.syntax._
import scala.concurrent.Future
import scala.util.Try


object SourceConverters {

  def toLogicJson[T](source: Source[T, NotUsed])(implicit encoder: Encoder[T]) =
    Future.successful( Try{
      val separators = Source.single("") ++ Source.repeat(",\n")
      val strings = (separators zipWith source){ case (sep, t) => sep + t.asJson.noSpaces }
      (Source.single("[\n") ++ strings ++ Source.single("\n]")).map(ByteString(_))
    }.toEither.swap.map{ e => println(s"Error: ${e.getMessage}") }.swap )

  def toLogicPlainText[T](source: Source[T, NotUsed]) =
    Future.successful( Try{
      source.map(_.toString + "\n").map(ByteString(_))
    }.toEither.swap.map{ e => println(s"Error: ${e.getMessage}") }.swap )

}
