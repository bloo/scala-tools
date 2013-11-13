package b.slick

/**
 * example Heroku URI:
 * 
 * heroku config DATABASE_URL
 * 
 * ---------|----------userInfo--------|-----------------------------------------|----|--------------
 *  scheme /|\    user      |  pass    |       host                              |port|  /path
 * --------+++--------------+----------+-----------------------------------------+----/--------------
 * postgres://ymhoznyjspdouz:By3wzE3bku@ec2-55-111-999-77.compute-1.amazonaws.com:5432/fdcgv1k7irje8a
 */
object URIDataSource extends b.log.Logger {

    def apply(uri: java.net.URI, min: Int, max: Int) = {
        DB(uri.getScheme) { dbc: DatabaseComponent =>

            val host = uri.getHost
            val userInfo = uri.getUserInfo.split(":")
            val user = userInfo(0)
            val pass = if (userInfo.length == 1) None else Some(userInfo(1))
            val port = if (uri.getPort <= -1) dbc.defaultPort else uri.getPort
            val jdbcUrl = "jdbc:%s://%s:%d%s" format (dbc.jdbcScheme, host, port, uri.getPath)
            
            PooledDataSource(dbc.driverName, jdbcUrl, user, pass, min, max)
        }
    }
}