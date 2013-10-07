package b.specs2.slick

import org.specs2._
import org.specs2.specification.AroundExample

trait TxRollback extends b.slick.Tx {

    import b.slick._
    import org.specs2.specification.{AroundOutside}
	import org.specs2.execute.{Result,AsResult}
    type S = Session
    object tx extends AroundOutside[S] {
        var sess: Option[S] = None
    	def around[R : AsResult](a: =>R): Result = tryThenRollback {s => {sess = Some(s); AsResult(a)}}
    	def outside: S = sess.get
    }
}
