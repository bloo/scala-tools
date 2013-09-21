package b.slick

import org.joda.time.DateTime

import b.slick.ql._

case class Page[T](values: List[T], page: Int, pageSize: Int, offset: Int, total: Int)

abstract class Model[T <: BaseModel](tableName: String)
    extends Table[T](DB.db.entityName(tableName)) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc, O.NotNull)
    def version = column[Int]("version", O.NotNull, O.Default(0))
    def created = column[DateTime]("created")

    def * : scala.slick.lifted.ColumnBase[T]

    def forInsert: scala.slick.lifted.ColumnBase[T]
    def autoInc = forInsert returning *

    def find(id: Long)(implicit s: Session) = tableToQuery(this).where(_.id === id).map(_*).first

    def findOption(id: Long)(implicit s: Session) = tableToQuery(this).where(_.id === id).map(_*).firstOption

    def page(page: Int = 0, pageSize: Int = 10)(implicit s: Session): Page[T] = {
        val values = list(page, Some(pageSize))
        val offset = pageSize * page
        Page(values, page, pageSize, offset, values.size)
    }

    def all(offset: Option[Int] = None, limit: Option[Int] = None)(implicit s: Session): List[T] =  {
        list(offset.getOrElse(0), limit)
    }
    
    def list(page: Int = 0, pageSize: Option[Int] = None)(implicit s: Session): List[T] = {
        val q = tableToQuery(this)
        val total = q.length.run // System.currentTimeMillis() / 1000
        pageSize match {
            case Some(ps) => q.drop(ps*page).take(ps).map(_.*).list
            case None => q.map(_.*).list
        }
        // add filter
        //.sortByRuntimeValue(_.column _, orderBy)
        //      Page(values, page, pageSize, offset, total)
    }

    def insert(entity: T)(implicit s: Session) = autoInc.insert(entity)

    def insertAll(entities: Seq[T])(implicit s: Session) = autoInc.insertAll(entities: _*)

    def update(entity: T)(implicit s: Session) = tableQueryToUpdateInvoker(
        tableToQuery(this).where(_.id === entity.id)).update(entity)

    def delete(entity: T)(implicit s: Session) = queryToDeleteInvoker(
        tableToQuery(this).where(_.id === entity.id)).delete

    def count(implicit s: Session) = Query(tableToQuery(this).length).first

    def count(filter: Model[T] with Table[T] => Boolean)(implicit s: Session) = Query(tableToQuery(this).filter(filter).length).first

}

trait BaseModel {
    val id: Option[Long]
    val version: Option[Int]
    val created: Option[DateTime]
}
