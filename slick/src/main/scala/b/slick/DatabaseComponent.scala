package b.slick

import scala.slick.session.ResultSetConcurrency
import scala.slick.session.ResultSetHoldability
import scala.slick.session.ResultSetType
import scala.slick.session.Database
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import javax.validation.ConstraintViolationException

trait Tx extends TxBase with DefaultSlickSessionComponent

object DB {

    def apply[D <: DatabaseComponent](dbComp: D) {
        DB.dbConfig = Some(dbComp)
        DB.db // init on 'apply' for fast-fail
    }

    def heroku(uri: java.net.URI) = {
        val userInfo = uri.getUserInfo.split(":")
        val user = userInfo(0)
        val port = uri.getPort
        implicit val dc = DatabaseConnect(user, uri.getHost, uri.getPath,
        		if (userInfo.length > 1) Some(userInfo(1)) else None,
                if (port == -1) None else Some(port))
        
        uri.getScheme match {
            case "postgres" => PostgreSQL
            case "h2" => H2
            case "mysql" => MySQL       
            case _ => throw new Error("Unable to create DatabaseConnect for %s" format uri.getScheme)
        } 
    }
    
    private var dbConfig: Option[DatabaseComponent] = None

    lazy val db = DB.dbConfig match {
        case Some(dbc) => dbc
        case None => throw new Error("Configure with: b.slick.DB(b.slick.DatabaseConnect)")
    }
}

trait SlickSessionComponent {
    def sessionProvider: SlickSessionProvider
    trait SlickSessionProvider {
        def createReadOnlySession(handle: Database): Session
        def createReadWriteSession(handle: Database): Session
    }
}

trait DefaultSlickSessionComponent extends SlickSessionComponent {
    def sessionProvider = new SlickSessionProvider {
        def createReadOnlySession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        }
        def createReadWriteSession(handle: Database): Session = {
            handle.createSession().forParameters(rsConcurrency = ResultSetConcurrency.ReadOnly)
        }
    }
}

trait TxBase extends b.common.Logger { this: SlickSessionComponent =>

    def db = DB.db

    import java.sql.Connection
    import DBSessionImplicits._
    import scala.concurrent._
    import ExecutionContext.Implicits.global

    def readOnlyAsync[T](f: ROSession => T): Future[T] = future { readOnly(f) }
    def readWriteAsync[T](f: RWSession => T): Future[T] = future { readWrite(f) }
    def readWriteAsync[T](attempts: Int)(f: RWSession => T): Future[T] = future { readWrite(attempts)(f) }

    def readOnly[T](f: ROSession => T): T = {
        var s: Option[Session] = None
        val ro = new ROSession({
            s = Some(sessionProvider.createReadOnlySession(db.handle))
            s.get
        })
        try f(ro) finally s.foreach(_.close())
    }

    def readWrite[T](f: RWSession => T): T = {
        val s = sessionProvider.createReadWriteSession(db.handle)
        try {
            s.withTransaction {
                f(new RWSession(s))
            }
        } finally s.close()
    }

    def doThenRollback[T](f: RWSession => T): T = {
        val s = sessionProvider.createReadWriteSession(db.handle)
        try {
            s.withTransaction {
                try {
                    f(new RWSession(s))
                } finally {
                    s.rollback()
                }
            }
        }
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

    abstract class SessionWrapper(_session: => Session) extends Session {
        lazy val session = _session

        def conn: Connection = session.conn
        def metaData = session.metaData
        def capabilities = session.capabilities
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

    abstract class RSession(roSession: => Session) extends SessionWrapper(roSession)
    class ROSession(roSession: => Session) extends RSession(roSession)
    class RWSession(rwSession: Session) extends RSession(rwSession)

    implicit def roToSession(roSession: ROSession): Session = roSession.session
    implicit def rwToSession(rwSession: RWSession): Session = rwSession.session
}

import scala.slick.driver.ExtendedDriver
import scala.slick.driver.H2Driver
import scala.slick.driver.MySQLDriver
import scala.slick.driver.PostgresDriver

case class DatabaseConnect(val user: String, val host: String, val path: String,
        val pass: Option[String] = None, val port: Option[Int] = None)

sealed trait DatabaseComponent extends b.common.Logger {

    // the Slick driver (e.g. H2 or MySQL)
    val driver: ExtendedDriver

    // connection info
    val dbc: DatabaseConnect
    val jdbcScheme: String
    def defaultPort: Int

    lazy val handle: scala.slick.session.Database = {
		val jdbcUrl = "jdbc:%s://%s:%d%s" format (jdbcScheme, dbc.host, dbc.port.getOrElse(defaultPort), dbc.path)
		logger.info("Creating database handle from: url=%s, user=%s, pass=***" format(jdbcUrl, dbc.user))
        dbc.pass match {
            case Some(p) => slick.session.Database.forURL(jdbcUrl, dbc.user, p)
            case None => slick.session.Database.forURL(jdbcUrl, dbc.user)
        }
    }
    
    // MySQL and H2 have different preferences on casing the table and column names.
    // H2 specifically prefers upper case
    def entityName(name: String): String = name
}

case class H2(val dbc: DatabaseConnect) extends DatabaseComponent {
    val driver = H2Driver
    val jdbcScheme = "h2"
    def defaultPort = throw new Error("Please configure an explicit port for H2 database")
    override def entityName(name: String): String = name.toUpperCase()
}

case class MySQL(val dbc: DatabaseConnect) extends DatabaseComponent {
    val driver = MySQLDriver
    val jdbcScheme = "mysql"
    def defaultPort = 3306
}

case class PostgreSQL(val dbc: DatabaseConnect) extends DatabaseComponent {
    val driver = PostgresDriver
    val jdbcScheme = "postgresql"
    def defaultPort = 5432
}
