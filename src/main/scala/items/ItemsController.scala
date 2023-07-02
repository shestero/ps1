package items

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.Source
import slick.jdbc.{JdbcProfile, ResultSetConcurrency, ResultSetType}
import sttp.capabilities.akka.AkkaStreams

import scala.concurrent.Future
import scala.language.postfixOps
import routing.SourceConverters._

abstract class ItemsController(implicit profile: JdbcProfile, session: SlickSession) extends routing.GetRouter[Item] {

  import profile.api._
  private val table: TableQuery[Items] = TableQuery[Items]
  private def queryId(id: Long) = table.filter(_.id === id)

  override def all: Source[Item, NotUsed] =
    Slick.source(
      table.result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 1000)
    )

  override def get(id: Long): Source[Item, NotUsed] =
    Slick.source(
      queryId(id).result.withStatementParameters(
        rsType = ResultSetType.ForwardOnly,
        rsConcurrency = ResultSetConcurrency.ReadOnly,
        fetchSize = 1000)
    )

  def getByPodcast(id: Long): Source[Item, NotUsed] =
    Slick.source( table.filter(_.podcastId===id).sortBy(_.pubDate.desc).result )

}

