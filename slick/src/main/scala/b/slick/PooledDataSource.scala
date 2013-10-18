package b.slick

object PooledDataSource extends b.common.Logger {

    def apply(driverClassName: String, jdbcUrl: String, user: String, min: Int, max: Int) = {

        logger.info("Creating com.mchange.v2.c3p0.ComboPooledDataSource from: url=%s, user=%s, pass=***" format (jdbcUrl, user))
        val ds = new com.mchange.v2.c3p0.ComboPooledDataSource
        ds.setDriverClass(driverClassName)
        ds.setJdbcUrl(jdbcUrl)
        ds.setUser(user)
        ds.setMinPoolSize(min)
        ds.setMaxPoolSize(max)
        ds.setMaxStatements(max)
        ds
    }
}