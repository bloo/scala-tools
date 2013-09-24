package b.slick

import org.joda.time.DateTime

import b.slick.ql._

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

    type FilterAndSort = Query[this.type,_] => Query[this.type,_]
    
    def list(page: Option[Int] = None, pageSize: Option[Int] = None,
            query: FilterAndSort = {q=>q})(implicit s: Session): List[T] = {

        val q = query(tableToQuery(this))
        (pageSize match {
            case Some(ps) => q.drop(ps*(page.getOrElse(1)-1)).take(ps)
            case None => q
        }).map(_.*).list
    }

    def count(query: FilterAndSort = {q=>q})(implicit s: Session) = {
        query(tableToQuery(this)).length.run
    }

    def insert(entity: T)(implicit s: Session) = autoInc.insert(entity)

    def insertAll(entities: Seq[T])(implicit s: Session) = autoInc.insertAll(entities: _*)

    def update(entity: T)(implicit s: Session) = tableQueryToUpdateInvoker(
        tableToQuery(this).where(_.id === entity.id)).update(entity)

    def delete(entity: T)(implicit s: Session) = queryToDeleteInvoker(
        tableToQuery(this).where(_.id === entity.id)).delete


}

trait BaseModel {
    val id: Option[Long]
    val version: Option[Int]
    val created: Option[DateTime]
}
