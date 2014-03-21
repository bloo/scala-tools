package b.slick

import java.sql.SQLException
import b.log.Logger
import javax.validation.ConstraintViolationException

trait Txn extends Logger with DB {

    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import simple._
    
    def txnAsync[T](f: Session => T): Future[T] = future { txn(f) }
    def txnAsync[T](attempts: Int)(f: Session => T): Future[T] = future { txn(attempts)(f) }

    def sess[T](f: Session => T): T = handle.withSession{ s => f(s) }
    
    def txn[T](f: Session => T): T = sess { s => try s.withTransaction { f(s) } }

    def withRollback[T](f: Session => T): T = sess { s =>
        s.withTransaction { try f(s) finally s.rollback() }
    }

    def txn[T](attempts: Int)(f: Session => T): T = {
        1 to attempts - 1 foreach { attempt =>
            try {
                return txn(f)
            } catch {
                case cve: ConstraintViolationException => throw cve
                case t: SQLException =>
                    val throwableName = t.getClass.getSimpleName
                    logger.warn(s"Failed ($throwableName) readWrite transaction attempt $attempt of $attempts")
            }
        }
        txn(f)
    }
}
