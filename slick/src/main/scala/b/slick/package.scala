package b

package object slick {

    import java.sql.Timestamp
    import org.joda.time.DateTime
    import org.joda.time.DateTimeZone.UTC
    import scala.slick.lifted.MappedTypeMapper.base
    import scala.slick.lifted.TypeMapper

    // sql.Timestamp <-> joda.DateTime type mapper
    //
    implicit val sql_2_joda_slickTypeMapper: TypeMapper[DateTime] =
        base[DateTime, Timestamp](
            d => new Timestamp(d getMillis),
            t => new DateTime(t getTime, UTC))

    // simplify Session type
    //
    type Session = scala.slick.session.Session

    // import the slick driver's "query language" imports
    //
    val ql = DB.db.driver.simple

    // implicit converter that wraps a Query in a QueryPager
    // that can list its results using pagination params
    //
    import ql._
    implicit def query_2_queryPager[QQ, R](q: Query[QQ, _ <: R]) = new QueryPager(q)
    class QueryPager[QQ, R](q: Query[QQ, _ <: R]) {
        def paginate(pp: QueryPager.PagerParams)(implicit s: Session): List[R] = paginate(pp.page, pp.size)
        def paginate(page: Option[Int], size: Option[Int])(implicit s: Session) = {
            (size match {
                case Some(sz) => q.drop(sz * (page.getOrElse(1) - 1)).take(sz)
                case None => q
            }).list
        }
    }

    object QueryPager {
        case class PagerParams(page: Option[Int], size: Option[Int])
    }
}
