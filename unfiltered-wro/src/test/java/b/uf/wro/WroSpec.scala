package b.uf.wro

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2._
import org.specs2.mock._
import b.common.Logger

@RunWith(classOf[JUnitRunner])
class WroSpec extends Specification with Mockito with Logger {

	"Wro Worker" should {
	    
	    "render coffeescript into js" in {
	        success
	    }
	    
	    "render sass into css" in {
	        success
	    }
	    
	    "render sass and bourbon into css" in {
	        success
	    }
	}
}
