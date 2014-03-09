package b.slick.ds

import javax.sql.DataSource
import com.typesafe.config.Config
import java.net.URI
import b.slick.DB
import b.slick.DatabaseComponent
import b.slick.Schemes

object PooledDataSource extends DB.DataSourceConfig with b.log.Logger {

    def config(cfg: Config)(dbcFn: Schemes.Scheme => DatabaseComponent) = {
    	info(cfg.root().render())
    	val jdbcUrl = cfg getString "jdbc.url"
    	val urlForScheme = new URI(jdbcUrl.replaceAll("jdbc:", ""))
    	val scheme = Schemes.withName(urlForScheme.getScheme)
    	val dbc = dbcFn(scheme)
    	val user = cfg getString "jdbc.user"
    	val pass = if (cfg hasPath "jdbc.pass") Some(cfg getString "jdbc.pass") else None
    	val min = if (cfg hasPath "min") cfg getInt "min" else 1
    	val max = cfg getInt "max"
    	val url = new URI(jdbcUrl)
    	scheme -> apply(dbc.driverName, url, user, pass, min, max)
    }
    
    def apply(driverClassName: String, jdbcUrl: URI, user: String, pass: Option[String], min: Int, max: Int): DataSource = {
        logger.info("Creating com.mchange.v2.c3p0.ComboPooledDataSource from: url=%s, user=%s, pass=***" format (jdbcUrl, user))
        val ds = new com.mchange.v2.c3p0.ComboPooledDataSource
        ds setDriverClass driverClassName
        ds setJdbcUrl jdbcUrl.toString
        ds setUser user
        pass map { ds setPassword _ }
        ds setMinPoolSize min
        ds setMaxPoolSize max
        ds setMaxStatements (max*10)
        ds setMaxStatementsPerConnection 10 
        //ds.setNumHelperThreads(10)
        ds
    }
}