package b.slick

import java.sql.SQLException

import scala.slick.jdbc.ResultSetConcurrency



import b.log.Logger
import javax.validation.ConstraintViolationException

trait Tx extends TxBase with DefaultSlickSessionComponent

trait SlickSessionComponent extends DB {
	trait SessionProvider {
		def createReadOnlySession(handle: Database): Session
		def createReadWriteSession(handle: Database): Session
	}
	def sessionProvider: SessionProvider
}

trait DefaultSlickSessionComponent extends SlickSessionComponent {
    import simple._
    def sessionProvider = new SessionProvider {
        def createReadOnlySession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        }
        def createReadWriteSession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.Updatable)
        }
    }
}

trait TxBase extends Logger { this: SlickSessionComponent =>

    import java.sql.Connection
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import simple._

    def readOnlyAsync[T](f: ROSession => T): Future[T] = future { readOnly(f) }
    def readWriteAsync[T](f: RWSession => T): Future[T] = future { readWrite(f) }
    def readWriteAsync[T](attempts: Int)(f: RWSession => T): Future[T] = future { readWrite(attempts)(f) }

    def readOnly[T](f: ROSession => T): T = {
        var s: Option[Session] = None
        val ro = new ROSession({
            s = Some(sessionProvider.createReadOnlySession(handle))
            s.get
        })
        try f(ro) finally s.foreach(_.close())
    }

    def readWrite[T](f: RWSession => T): T = {
        val s = sessionProvider.createReadWriteSession(handle)
        try {
            s.withTransaction {
                f(new RWSession(s))
            }
        } finally s.close()
    }

    def tryThenRollback[T](f: RWSession => T): T = {
        val s = sessionProvider.createReadWriteSession(handle)
        try {
            s.withTransaction {
                try {
                    f(new RWSession(s))
                } finally {
                    s.rollback()
                }
            }
        } finally s.close()
    }

    def readWrite[T](attempts: Int)(f: RWSession => T): T = {
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
