package b.specs2.slick

import b.slick._
import org.specs2._
import org.specs2.specification.AroundExample
import com.typesafe.config.ConfigFactory

abstract class PostgresSpecBase(dbName: String) extends DBSpecBase(Schemes.postgresql, dbName)
abstract class MysqlSpecBase(dbName: String) extends DBSpecBase(Schemes.mysql, dbName)

abstract class DBSpecBase(jdbcScheme: Schemes.Scheme, dbName: String)
	extends mutable.Specification with Tx {

    import b.slick._
    import org.specs2.specification.{AroundOutside,Around}
	import org.specs2.execute.{Result,AsResult}

    object tx extends AroundOutside[Session] {
        var sess: Option[Session] = None
    	def around[R : AsResult](a: =>R): Result = tryThenRollback {s => {sess = Some(s); AsResult(a)}}
    	def outside: Session = sess.get
    }
}
