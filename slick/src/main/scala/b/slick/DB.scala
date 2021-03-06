package b.slick

import org.joda.time.DateTimeZone

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import b.log.Logger
import b.slick.ds.PooledJdbcDataSource
import b.slick.ds.PooledURIDataSource
import javax.sql.DataSource

object DB extends Logger {

	type Name = String
	import Schemes._

	private var _cfg: Option[Config] = None
	def config(c: Config) = _cfg = Some(c getConfig "b.database")
	def cfg: Config = _cfg match {
		case Some(c) => c
		case None => {
			config(ConfigFactory.load())
			cfg
		}
	}
	
	val CFG_DEFAULTNAME: Name = "default"

	private val _handles = collection.mutable.Map.empty[Name, scala.slick.jdbc.JdbcBackend.DatabaseDef]
	private val _components = collection.mutable.Map.empty[Name, DatabaseComponent]
	private val _configs = collection.mutable.Map.empty[Name, DataSourceConfig]
	import collection.JavaConversions._

	var configured = false
	private def config = this.synchronized {
		if (!configured) {
		
			// parse pool.uri, pool.jdbc, OR pools array
			//
			if (cfg hasPath "pool.uri") configEach(new PooledURIDataSource, cfg getConfig "pool")
			else if (cfg hasPath "pool.jdbc") configEach(new PooledJdbcDataSource, cfg getConfig "pool")
			else if (cfg hasPath "pools") cfg.getConfigList("pools").toList.foreach { poolCfg =>
				if (poolCfg hasPath "uri") configEach(new PooledURIDataSource, poolCfg)
				else if (poolCfg hasPath "jdbc") configEach(new PooledJdbcDataSource, poolCfg)
			}
			
			info("Configured Database names: " + _handles.keySet)
			configured = true
		}
	}
	
	def shutdown = _configs.values.foreach { _.shutdown }

	def configEach[C <: DataSourceConfig](c: C, cfgObj: Config) = {
		val name = if (cfgObj hasPath "name") cfgObj getString "name" else "default"
		if (_handles containsKey name) throw new Error(
			s"Cannot have more than one database configuration named: $name; taken names: " + _handles.keySet)
		val (scheme, ds) = c.init(cfgObj) { scheme => componentForScheme(scheme) }
		_configs(name) = c
		_components(name) = componentForScheme(scheme)
		_handles(name) = scala.slick.jdbc.JdbcBackend.Database.forDataSource(ds)
	}

	def componentForScheme(scheme: Scheme) = scheme match {
		case `postgres` | `postgresql` => PostgreSQL
		case `h2` => H2
		case `mysql` => MySQL
		case other => throw new Error(s"No DatabaseComponent for scheme $other")
	}

	abstract class DataSourceConfig {
		def init(cfg: Config)(schemaFn: Scheme => DatabaseComponent): (Scheme, DataSource)
		def shutdown
	}

	def component(name: Name) = _components get name match {
		case Some(comp) => comp
		case None => throw new Error(s"No DatabaseComponent named $name; available names: " + _components.keySet)
	}

	def handle(name: Name) = _handles get name match {
		case Some(db) => db
		case None => throw new Error(s"No Database handle named $name; available names: " + _handles.keySet)
	}
}

/*
 * TODO
 * if this is too heavy - all of these defs for each subclass of DB,
 * we should isolate it in such a way that the application explicitly
 * defines traits based on the DB driver/component as described in
 * the configuration. that means the app will have to expect what's
 * configured to work correctly.
 */
trait DB {

	DB.config
	
	// import the slick driver's "query language" imports
	//
	val name: DB.Name = DB.CFG_DEFAULTNAME
	val handle = DB.handle(name)
	val component = DB.component(name)
	//val simple = DB.component.driver.profile.simple
	val simple = component.driver.profile.simple

	// simplify Session type
	type Database = scala.slick.jdbc.JdbcBackend.Database
	type JdbcSession = scala.slick.jdbc.JdbcBackend.Session
	type Session = simple.Session

	import simple._
	import java.sql.Timestamp
	import org.joda.time.DateTime
	import org.joda.time.DateTimeZone.UTC
	import scala.slick.ast.BaseTypedType

	// sql.Timestamp <-> joda.DateTime type mapper
	//
	implicit val sql_2_jodaDateTime_slickTypeMapper =
		MappedColumnType.base[DateTime, Timestamp](
			d => new Timestamp(d getMillis),
			t => new DateTime(t getTime, UTC))

	// String <-> joda.DateTimeZone type mapper
	//
	implicit val sql_2_jodaDateTimeTZ_slickTypeMapper =
		MappedColumnType.base[DateTimeZone, String](
			tz => tz.getID,
			s => DateTimeZone.forID(s))

	abstract class DBTable[T](tag: Tag, tableName: String)
		extends Table[T](tag: Tag, component.entityName(tableName)) {}

	// implicit converters that wrap CompiledExcetuable or Query instances within a QueryPager
	// that will then list its results using pagination params via .paginate(PagerParams, QQ=>O)
	//
	implicit def query_2_pagedQuery[QQ, R](q: Query[QQ, _ <: R]) = new PagedQuery(q)
	import scala.slick.lifted.{ CompiledExecutable => CE, AppliedCompiledFunction => ACF }
	//    implicit def compiled_2_queryPager[QQ, R](c: CE[Query[QQ,_<:R],_]) = new QueryPager(c.extract)
	//    implicit def compiledf_2_queryPager[QQ, R](c: ACF[_,Query[_,_<:R],_]) = new QueryPager(c.extract)

	object PagedQuery {
		case class Page(page: Option[Int], size: Option[Int])
	}
	type Page = PagedQuery.Page

	import simple.{ Query, queryToAppliedQueryInvoker }
	class PagedQuery[QQ, R](q: Query[QQ, _ <: R]) extends b.log.Logger {
		def paginate(pp: Page)(implicit s: Session): List[R] = paginate(pp.page, pp.size, None)
		def paginate[O <% scala.slick.lifted.Ordered](pp: Page, sorter: QQ => O)(implicit s: Session): List[R] = paginate(pp.page, pp.size, Some(sorter))
		def paginate[O <% scala.slick.lifted.Ordered](page: Option[Int], size: Option[Int], sorterOpt: Option[QQ => O])(implicit s: Session) = {

			val sorted = sorterOpt match {
				case Some(sorter) => q sortBy sorter
				case None => q
			}

			val pq = (size match {
				case Some(sz) => {
					val ps = (page getOrElse 1) - 1
					sorted.drop(sz * (if (ps < 0) 0 else ps)).take(sz)
				}
				case None => sorted
			})
			//            if (logger.isDebugEnabled)
			//                logger.debug(pq.selectStatement)
			pq.list
		}
	}
}
