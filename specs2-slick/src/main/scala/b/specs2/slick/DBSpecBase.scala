package b.specs2.slick

import b.slick._
import org.specs2._
import org.specs2.specification.AroundExample
import com.typesafe.config.ConfigFactory
import java.util.UUID

abstract class PostgresSpecBase extends DBSpecBase(Schemes.postgresql)
abstract class MysqlSpecBase extends DBSpecBase(Schemes.mysql)

abstract class DBSpecBase(jdbcScheme: Schemes.Scheme)
	extends mutable.Specification with Tx {

    import b.slick._
    import org.specs2.specification.{AroundOutside,Around}
	import org.specs2.execute.{Result,AsResult}

    object tx extends AroundOutside[Session] {
        var sess: Option[Session] = None
    	def around[R : AsResult](a: =>R): Result = tryThenRollback {s => {sess = Some(s); AsResult(a)}}
    	def outside: Session = sess.get
    }

    object ro extends AroundOutside[Session] {
        var sess: Option[Session] = None
    	def around[R : AsResult](a: =>R): Result = readOnly {s => {sess = Some(s); AsResult(a)}}
    	def outside: Session = sess.get
    }
    
    def randomTableName = UUID.randomUUID().toString().replaceAll("-", "")
}
