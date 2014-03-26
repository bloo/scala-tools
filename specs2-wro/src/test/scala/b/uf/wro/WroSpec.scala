package b.uf.wro

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2._
import org.specs2.mock._
import b.log.Logger

@RunWith(classOf[JUnitRunner])
class WroSpec extends Specification with Mockito with Logger {

	"Wro Worker" should {
		
		System.setProperty("b.wro.hostedUrl", "/")
		val debug = Wro.config.debug
	    
	    "render coffeescript into js" in {
	        logger info "render coffeescript!"
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
