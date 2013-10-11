package b.slick

import org.joda.time.DateTime

import b.slick.ql._
import scala.slick.lifted.ColumnBase

abstract class DBTable[T](tableName: String) extends Table[T](DB.db.entityName(tableName)) {}

abstract class Model[T <: {val id: Option[Long]}](tableName: String) extends DBTable[T](tableName) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc, O.NotNull)

    def * : ColumnBase[T]
    def forInsert: ColumnBase[T] = *
    def autoInc = forInsert returning *

    type FilterAndSort = Query[this.type,_] => Query[this.type,_]

    def find(id: Long)(implicit s: Session) =
        tableToQuery(this).where(_.id === id).map(_*).first    

    def findOption(id: Long)(implicit s: Session) =
        tableToQuery(this).where(_.id === id).map(_*).firstOption

    def update(entity: T)(implicit s: Session) = tableQueryToUpdateInvoker(
        tableToQuery(this).where(_.id === entity.id)).update(entity)
        
    def delete(entity: T)(implicit s: Session) = queryToDeleteInvoker(
        tableToQuery(this).where(_.id === entity.id)).delete
        
    def list(page: Option[Int] = None, pageSize: Option[Int] = None,
            query: FilterAndSort = {q=>q})(implicit s: Session): List[_] =
                query(tableToQuery(this)).paginate(page, pageSize)

    def count(query: FilterAndSort = {q=>q})(implicit s: Session) =
        query(tableToQuery(this)).length.run

    def insert(entity: T)(implicit s: Session) =
        autoInc.insert(entity)

    def insert[I](entity: I, insertProj: ColumnBase[I])(implicit s: Session) =
        insertProj returning * insert entity
        
    def insertAll(entities: Seq[T])(implicit s: Session) =
        autoInc.insertAll(entities: _*)
        
    def insertAll[I](entities: Seq[I], insertProj: ColumnBase[I])(implicit s: Session) =
        insertProj returning * insertAll(entities: _*)
}

