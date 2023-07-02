package channels

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}

import scala.language.postfixOps
import Channel._

abstract class ChannelsController(implicit profile: JdbcProfile, session: SlickSession) extends routing.GetRouter[Channel] {

  import profile.api._
  private val table: TableQuery[Channels] = TableQuery[Channels]
  private def queryId(id: Long) = table.filter(_.id === id)

  override def all: Source[Channel, NotUsed] =
    Slick.source(
      table.result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 1000)
    )

  override def get(id: Long): Source[Channel, NotUsed] =
    Slick.source(
      queryId(id).result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 1000)
    )
}
