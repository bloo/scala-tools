package b.specs2.slick

import b.slick._
import org.specs2._
import org.specs2.specification.AroundExample

abstract class PostgresSpecBase(dbName: String) extends DBSpecBase("org.postgresql.Driver", "postgresql", dbName)
abstract class DBSpecBase(driver: String, jdbcScheme: String, dbName: String) extends mutable.Specification with TxRollback {
    val un = System.getProperty("user.name")
    val dataSource = new org.apache.commons.dbcp.BasicDataSource
    dataSource.setDriverClassName("org.postgresql.Driver")
    dataSource.setUrl("jdbc:%s://localhost/%s" format (jdbcScheme,dbName))
    dataSource.setUsername(un)
    // init
    DB(jdbcScheme, dataSource)
}
