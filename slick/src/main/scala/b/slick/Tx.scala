package b.slick

import java.sql.SQLException

import scala.slick.jdbc.ResultSetConcurrency



import b.log.Logger
import javax.validation.ConstraintViolationException

trait Tx extends TxBase with DB



trait TxBase extends Logger { this: DB =>

    import java.sql.Connection
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import simple._
    
    def readOnlyAsync[T](f: Session => T): Future[T] = future { readOnly(f) }
    def readWriteAsync[T](f: Session => T): Future[T] = future { readWrite(f) }
    def readWriteAsync[T](attempts: Int)(f: Session => T): Future[T] = future { readWrite(attempts)(f) }

    def readOnly[T](f: Session => T): T = {
    	val s = handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        try f(s) finally s.close()
    }

    def readWrite[T](f: Session => T): T = {
        val s = handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.Updatable)
        try s.withTransaction { f(s) } finally s.close()
    }

    def tryThenRollback[T](f: Session => T): T = {
        val s = handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.Updatable)
        try s.withTransaction {
        	try f(s)
        	finally s.rollback()
        } finally s.close()
    }

    def readWrite[T](attempts: Int)(f: Session => T): T = {
        1 to attempts - 1 foreach { attempt =>
            try {
                return readWrite(f)
            } catch {
                case cve: ConstraintViolationException => throw cve
                case t: SQLException =>
                    val throwableName = t.getClass.getSimpleName
                    logger.warn(s"Failed ($throwableName) readWrite transaction attempt $attempt of $attempts")
            }
        }
        readWrite(f)
    }
}
