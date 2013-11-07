package b

package object slick {

    object QueryPager {
        case class PagerParams(page: Option[Int], size: Option[Int])
    }

    import DB.component.driver.profile.simple.{ Session, Query, queryToAppliedQueryInvoker }

    class QueryPager[QQ, R](q: Query[QQ, _ <: R]) {
        def paginate(pp: QueryPager.PagerParams)(implicit s: Session): List[R] = paginate(pp.page, pp.size)
        def paginate(page: Option[Int], size: Option[Int])(implicit s: Session) = {
            (size match {
                case Some(sz) => q.drop(sz * (page.getOrElse(1) - 1)).take(sz)
                case None => q
            }).list
        }
    }

    // import the slick driver's "query language" imports
    //
    val simple = DB.component.driver.profile.simple

    // simplify Session type
    type Database = scala.slick.jdbc.JdbcBackend.Database
    type JdbcSession = scala.slick.jdbc.JdbcBackend.Session
    type Session = simple.Session

    import simple._
    import java.sql.Timestamp
    import org.joda.time.DateTime
    import org.joda.time.DateTimeZone.UTC
    import scala.slick.ast.BaseTypedType

    // sql.Timestamp <-> joda.DateTime type mapper
    //
    implicit val sql_2_joda_slickTypeMapper: BaseTypedType[DateTime] =
        MappedColumnType.base[DateTime, Timestamp](
            d => new Timestamp(d getMillis),
            t => new DateTime(t getTime, UTC)
        	)
        	
    abstract class DBTable[T](tag: Tag, tableName: String)
    	extends Table[T](tag: Tag, DB.component.entityName(tableName)) {}
    
    // implicit converter that wraps a Query in a QueryPager
    // that can list its results using pagination params
    //
    implicit def query_2_queryPager[QQ, R](q: Query[QQ, _ <: R]) = new QueryPager(q)
}
