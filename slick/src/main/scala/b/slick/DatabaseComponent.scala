package b.slick

import scala.slick.driver.H2Driver
import scala.slick.driver.JdbcDriver
import scala.slick.driver.MySQLDriver
import scala.slick.driver.PostgresDriver
import b.log.Logger

object Schemes extends Enumeration {
  type Scheme = Value
  val postgres, postgresql, h2, mysql = Value
}

sealed trait DatabaseComponent extends Logger {

    // the Slick driver (e.g. H2 or MySQL)
    val driver: JdbcDriver
    val driverName: String

    // connection info
    def defaultPort: Int
    val jdbcUrlScheme: String

    // MySQL and H2 have different preferences on casing the table and column names.
    // H2 specifically prefers upper case
    def entityName(name: String): String = name
}
	
object H2 extends DatabaseComponent {
    val driver = H2Driver
    val jdbcUrlScheme = "h2"
    val driverName = "org.h2.Driver"
    def defaultPort = throw new Error("Please configure an explicit port for H2 database")
    override def entityName(name: String): String = name.toUpperCase()
}

object MySQL extends DatabaseComponent {
    val driver = MySQLDriver
    val jdbcUrlScheme = "mysql"
    val driverName = "com.mysql.jdbc.Driver"
    def defaultPort = 3306
}

object PostgreSQL extends DatabaseComponent {
    val driver = PostgresDriver
    val jdbcUrlScheme = "postgresql"
    val driverName = "org.postgresql.Driver"
    def defaultPort = 5432
}
