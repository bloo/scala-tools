package b.slick.ds

import java.net.URI
import com.typesafe.config.Config
import b.slick.DB
import b.slick.DatabaseComponent
import b.slick.Schemes

/**
 * Convert a URI to a JDBC URL.
 * 
 * example Heroku URI:
 * 
 * $ heroku config DATABASE_URL
 * 
 * ---------|----------userInfo--------|-----------------------------------------|----|--------------
 *  scheme /|\    user      |  pass    |       host                              |port|  /path
 * --------+++--------------+----------+-----------------------------------------+----/--------------
 * postgres://ymhoznyjspdouz:By3wzE3bku@ec2-55-111-999-77.compute-1.amazonaws.com:5432/fdcgv1k7irje8a
 */
object PooledURIDataSource extends DB.DataSourceConfig with b.log.Logger {

    def config(cfg: Config)(dbcFn: Schemes.Scheme => DatabaseComponent) = {
    	val uri = new URI(cfg getString "uri")
    	val scheme = Schemes.withName(uri.getScheme)
    	val dbc = dbcFn(scheme)
        val host = uri.getHost
        val userInfo = uri.getUserInfo.split(":")
        val user = userInfo(0)
        val pass = if (userInfo.length == 1) None else Some(userInfo(1))
        val port = if (uri.getPort <= -1) dbc.defaultPort else uri.getPort
        val min = if (cfg hasPath "min") Some(cfg getInt "min") else None
        val max = cfg getInt "max"
        val jdbcUrl = new URI("jdbc:%s://%s:%d%s" format (dbc.jdbcUrlScheme, host, port, uri.getPath))
        scheme -> PooledJdbcDataSource(dbc.driverName, jdbcUrl, Some(user), pass, min, max)
    }
}
