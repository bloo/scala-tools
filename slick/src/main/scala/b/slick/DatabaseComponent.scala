package b.slick

import b.log.Logger
import scala.slick.jdbc.ResultSetConcurrency
import scala.slick.jdbc.ResultSetHoldability
import scala.slick.jdbc.ResultSetType
import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicDriver
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.validation.ConstraintViolationException

trait Tx extends TxBase with DefaultSlickSessionComponent

object DB extends Logger {

    private var comp: DatabaseComponent = null
    lazy val component = comp
    private var db: Database = null
    lazy val handle = db
    
    def apply(scheme: String)(dcf: DatabaseComponent => javax.sql.DataSource) = {
        comp = scheme match {
            case "postgres" | "postgresql" => PostgreSQL
            case "h2" => H2
            case "mysql" => MySQL
            case _ => throw new Error("Unable to create DatabaseConnect for %s" format scheme)
        }
        val ds = dcf(comp)
        db = scala.slick.jdbc.JdbcBackend.Database.forDataSource(ds)
    }
}

trait SlickSessionComponent {
    import simple._
    def sessionProvider: SlickSessionProvider
    trait SlickSessionProvider {
        def createReadOnlySession(handle: Database): Session
        def createReadWriteSession(handle: Database): Session
    }
}

trait DefaultSlickSessionComponent extends SlickSessionComponent {
    import simple._
    def sessionProvider = new SlickSessionProvider {
        def createReadOnlySession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        }
        def createReadWriteSession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        }
    }
}

trait TxBase extends Logger { this: SlickSessionComponent =>

    import java.sql.Connection
    import DBSessionImplicits._
    import scala.concurrent._
    import ExecutionContext.Implicits.global
    import simple._

    def readOnlyAsync[T](f: ROSession => T): Future[T] = future { readOnly(f) }
    def readWriteAsync[T](f: RWSession => T): Future[T] = future { readWrite(f) }
    def readWriteAsync[T](attempts: Int)(f: RWSession => T): Future[T] = future { readWrite(attempts)(f) }

    lazy val handle = DB.handle

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

object DBSessionImplicits {
    import simple._

    abstract class SessionWrapper(_session: => Session) extends JdbcSession {
        lazy val session: JdbcSession = _session.asInstanceOf[JdbcSession]

        def conn: Connection = session.conn
        def metaData = session.metaData
        def capabilities = session.capabilities
        override def database = session.database
        override def resultSetType = session.resultSetType
        override def resultSetConcurrency = session.resultSetConcurrency
        override def resultSetHoldability = session.resultSetHoldability
        def close() { throw new UnsupportedOperationException }
        def rollback() { session.rollback() }
        def withTransaction[T](f: => T): T = session.withTransaction(f)

        private val statementCache = new scala.collection.mutable.HashMap[String, PreparedStatement]
        def getPreparedStatement(statement: String): PreparedStatement =
            statementCache.getOrElseUpdate(statement, this.conn.prepareStatement(statement))

        override def forParameters(rsType: ResultSetType = resultSetType, rsConcurrency: ResultSetConcurrency = resultSetConcurrency,
            rsHoldability: ResultSetHoldability = resultSetHoldability) = throw new UnsupportedOperationException
    }

    abstract class RSession(rSession: => Session) extends SessionWrapper(rSession)
    class ROSession(roSession: => Session) extends RSession(roSession)
    class RWSession(rwSession: Session) extends RSession(rwSession)

    implicit def roToSession(roSession: ROSession): Session = roSession.session
    implicit def rwToSession(rwSession: RWSession): Session = rwSession.session
}



import scala.slick.driver.JdbcDriver
sealed trait DatabaseComponent extends Logger {

    // the Slick driver (e.g. H2 or MySQL)
    val driver: JdbcDriver
    val driverName: String

    // connection info
    def defaultPort: Int
    val jdbcScheme: String

    // MySQL and H2 have different preferences on casing the table and column names.
    // H2 specifically prefers upper case
    def entityName(name: String): String = name
}

import scala.slick.driver.H2Driver
import scala.slick.driver.MySQLDriver
import scala.slick.driver.PostgresDriver

object H2 extends DatabaseComponent {
    val driver = H2Driver
    val jdbcScheme = "h2"
    val driverName = "org.h2.Driver"
    def defaultPort = throw new Error("Please configure an explicit port for H2 database")
    override def entityName(name: String): String = name.toUpperCase()
}

object MySQL extends DatabaseComponent {
    val driver = MySQLDriver
    val jdbcScheme = "mysql"
    val driverName = "com.mysql.jdbc.Driver"
    def defaultPort = 3306
}

object PostgreSQL extends DatabaseComponent {
    val driver = PostgresDriver
    val jdbcScheme = "postgresql"
    val driverName = "org.postgresql.Driver"
    def defaultPort = 5432
}
