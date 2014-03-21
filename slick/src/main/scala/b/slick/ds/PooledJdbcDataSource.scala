package b.slick.ds

import javax.sql.DataSource
import com.typesafe.config.Config
import java.net.URI
import b.slick.DB
import b.slick.DatabaseComponent
import b.slick.Schemes

class PooledJdbcDataSource extends DB.DataSourceConfig with b.log.Logger {

	val ds = new org.apache.commons.dbcp2.BasicDataSource()
	
    def init(cfg: Config)(dbcFn: Schemes.Scheme => DatabaseComponent) = {
    	info(cfg.root().render())
    	val jdbcUrl = cfg getString "jdbc.url"
    	val urlForScheme = new URI(jdbcUrl.replaceAll("jdbc:", ""))
    	val scheme = Schemes.withName(urlForScheme.getScheme)
    	val dbc = dbcFn(scheme)
    	val user = if (cfg hasPath "jdbc.user") Some(cfg getString "jdbc.user") else None
    	val pass = if (cfg hasPath "jdbc.pass") Some(cfg getString "jdbc.pass") else None
    	val min = if (cfg hasPath "min") Some(cfg getInt "min") else None
    	val max = cfg getInt "max"
    	val url = new URI(jdbcUrl)
    	scheme -> init(dbc.driverName, url, user, pass, min, max)
    }
    
    def init(driverClassName: String, jdbcUrl: URI, user: Option[String], pass: Option[String], min: Option[Int], max: Int): DataSource = {
        logger.info(s"$ds.className from: url=$jdbcUrl, user=$user, pass=***")
        user map { ds setUsername _ }
        pass map { ds setPassword _ }
        ds setUrl jdbcUrl.toString
        ds setDriverClassName driverClassName
        ds setMaxTotal max
        ds setMinIdle (min match {
        	case Some(m) => m
        	case None => 1
        })
        ds
    }
    
    def shutdown = ds.close()
}