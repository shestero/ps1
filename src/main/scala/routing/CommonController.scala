package routing

import akka.stream.scaladsl.Source
import akka.util.ByteString
import slick.jdbc.PostgresProfile.api._
import sttp.capabilities.akka.AkkaStreams
import podcasts.Podcasts
import channels.Channels
import items.Items
import util.Generator

import scala.concurrent.Future

class CommonController {

  val logicSQLSchema: Unit => Future[Either[Unit, AkkaStreams.BinaryStream]] = _ => {
    val schemas = List(
      TableQuery[Podcasts].schema,
      TableQuery[Channels].schema,
      TableQuery[Items].schema,
      TableQuery[Generator].schema
    )
    val sql = schemas.flatMap(_.createIfNotExistsStatements).map(_.replace("\"","")+ ";\n")
    Future.successful( Right( Source( sql.map(ByteString(_)) ) ) )
  }


}
