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
object URIDataSource extends b.common.Logger {

    def apply(uri: java.net.URI, min: Int, max: Int) = {
        DB(uri.getScheme) { dbc: DatabaseComponent =>

            val host = uri.getHost
            val userInfo = uri.getUserInfo.split(":")
            val user = userInfo(0)
            val pass = if (userInfo.length == 1) None else Some(userInfo(1))
            val port = if (uri.getPort <= -1) dbc.defaultPort else uri.getPort
            val dbName = uri.getPath.replaceAll("^/", "")

            val jdbcUrl = "jdbc:%s://%s:%d/%s" format (dbc.jdbcScheme, host, port, dbName)
            logger.info("Creating DataSource from: url=%s, user=%s, pass=***" format (jdbcUrl, user))

            val ds = new org.apache.commons.dbcp.BasicDataSource
            ds.setDriverClassName(dbc.driverName)
            ds.setUrl(jdbcUrl)
            ds.setUsername(user)
            // http://commons.apache.org/proper/commons-dbcp/configuration.html
            // initialSize	0	 The initial number of connections that are created when the pool is started. 
            // maxActive	8	 The maximum number of active connections that can be allocated from this pool at the same time, or negative for no limit.
            // maxIdle		8	 The maximum number of connections that can remain idle in the pool, without extra ones being released, or negative for no limit.
            // minIdle		0	 The minimum number of connections that can remain idle in the pool, without extra ones being created, or zero to create none.
            // maxWait		∞	 The maximum number of milliseconds that the pool will wait (when there are no available connections) for a connection to be returned before throwing an exception, or -1 to wait indefinitely.
            ds.setMinIdle(min)
            ds.setMaxIdle(max)
            ds.setMaxActive(max)
            ds
        }
    }
}