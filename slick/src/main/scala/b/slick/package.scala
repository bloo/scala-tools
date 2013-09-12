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
}
