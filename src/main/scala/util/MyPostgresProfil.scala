package util

import com.github.tminglei.slickpg.utils.SimpleArrayUtils
import com.github.tminglei.slickpg.{ExPostgresProfile, PgArraySupport}
import play.api.libs.json.{JsValue, Json}


trait MyPostgresProfile extends ExPostgresProfile
  with PgArraySupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[slick.basic.Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  object MyAPI extends API /* ExtPostgresAPI */ with ArrayImplicits
  {
    implicit val strListTypeMapper: DriverJdbcType[Seq[String]] =
      new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val playJsonArrayTypeMapper: DriverJdbcType[Seq[JsValue]] =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        s => Option(s).filter(_.nonEmpty).flatMap(s => SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s)).orNull,
        v => SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)

  }

  override val api = MyAPI
}

object MyPostgresProfile extends MyPostgresProfile