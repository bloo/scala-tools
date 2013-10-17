package b.specs2.slick

import b.slick._
import org.specs2._
import org.specs2.specification.AroundExample

abstract class PostgresSpecBase(dbName: String) extends DBSpecBase("postgresql", dbName)
abstract class DBSpecBase(jdbcScheme: String, dbName: String) extends mutable.Specification with TxRollback {
    // init
    DB(jdbcScheme) { dbc: DatabaseComponent =>
	    val un = System.getProperty("user.name")
	    val ds = new org.apache.commons.dbcp.BasicDataSource
	    ds.setDriverClassName(dbc.driverName)
	    ds.setUrl("jdbc:%s://localhost/%s" format (dbc.jdbcScheme, dbName))
	    ds.setUsername(un)
	    ds
    }
}
