package b

import org.joda.time.DateTimeZone
package object slick {

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
    implicit val sql_2_jodaDateTime_slickTypeMapper =
        MappedColumnType.base[DateTime, Timestamp](
            d => new Timestamp(d getMillis),
            t => new DateTime(t getTime, UTC))

    // String <-> joda.DateTimeZone type mapper
    //
    implicit val sql_2_jodaDateTimeTZ_slickTypeMapper =
        MappedColumnType.base[DateTimeZone, String](
            tz => tz.getID,
            s => DateTimeZone.forID(s))

    abstract class DBTable[T](tag: Tag, tableName: String)
        extends Table[T](tag: Tag, DB.component.entityName(tableName)) {}

    // implicit converters that wrap CompiledExcetuable or Query instances within a QueryPager
    // that will then list its results using pagination params via .paginate(PagerParams, QQ=>O)
    //
    implicit def query_2_queryPager[QQ, R](q: Query[QQ, _<:R]) = new QueryPager(q)
    import scala.slick.lifted.{CompiledExecutable=>CE,AppliedCompiledFunction=>ACF}
//    implicit def compiled_2_queryPager[QQ, R](c: CE[Query[QQ,_<:R],_]) = new QueryPager(c.extract)
//    implicit def compiledf_2_queryPager[QQ, R](c: ACF[_,Query[_,_<:R],_]) = new QueryPager(c.extract)
    
    object QueryPager {
        case class Page(page: Option[Int], size: Option[Int])
    }
    type Page = QueryPager.Page

    import simple.{ Query, queryToAppliedQueryInvoker }
    class QueryPager[QQ, R](q: Query[QQ, _ <: R]) extends b.log.Logger {
        def paginate[O <% scala.slick.lifted.Ordered](pp: Page, sorter: QQ=>O)(implicit s: Session): List[R] = paginate(pp.page, pp.size, sorter)
        def paginate[O <% scala.slick.lifted.Ordered](page: Option[Int], size: Option[Int], sorter: QQ=>O)(implicit s: Session) = {
            val sorted = q sortBy sorter
            val pq = (size match {
                case Some(sz) => {
                    val ps = (page getOrElse 1) - 1
                    sorted.drop(sz * (if (ps < 0) 0 else ps)).take(sz)
                }
                case None => sorted
            })
//            if (logger.isDebugEnabled)
//                logger.debug(pq.selectStatement)
            pq.list
        }
    }
}
