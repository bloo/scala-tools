package b.slick

object PooledDataSource extends b.common.Logger {

    def apply(driverClassName: String, jdbcUrl: String, user: String, password: Option[String], min: Int, max: Int) = {

        logger.info("Creating com.mchange.v2.c3p0.ComboPooledDataSource from: url=%s, user=%s, pass=***" format (jdbcUrl, user))
        val ds = new com.mchange.v2.c3p0.ComboPooledDataSource
        ds.setDriverClass(driverClassName)
        ds.setJdbcUrl(jdbcUrl)
        ds.setUser(user)
        if (password.isDefined) ds.setPassword(password.get)
        ds.setMinPoolSize(min)
        ds.setMaxPoolSize(max)
        ds.setMaxStatements(max*10)
        ds.setMaxStatementsPerConnection(10)
        //ds.setNumHelperThreads(10)
        ds
    }
}