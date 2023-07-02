package util

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.ExecutionContext

class Generator(tag: Tag) extends Table[Long](tag, "generator") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  override def * = (id)
}

object Generator {

  def next(implicit ec: ExecutionContext): DBIO[Long] = {
    val items: TableQuery[Generator] = TableQuery[Generator]
    for {
      maxOp <- items.map(_.id).max.result
      max = maxOp.getOrElse(0L)
      r <- items.map(_.id).returning(items.map(_.id)).insertOrUpdate( max+1 )
    } yield r.get
  }.transactionally

}