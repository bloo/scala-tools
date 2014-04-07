package b.specs2.wro

import org.junit.runner.RunWith
import org.specs2._
import org.specs2.mock._
import org.specs2.runner.JUnitRunner
import b.uf.wro.WroPlans
import b.uf.wro.WroGenerator

@RunWith(classOf[JUnitRunner])
class WroSpec extends WroSpecBase {

	"WroGenerator" should {

	    "generate test resources" in wro { gen: Gen =>

	    	import WroPlans._
	    	val routes = "/public/assets/rsrc/test" -> appPlan(file("src/test/resources/wro-test.xml"))
	    	val out = gen(Seq(routes))

	    	info("XXXXXKASDFHADF")
	    	out foreach { path => println(s"path: $path") }
	    	
	    	// we tested more than one file
	    	out.length > 0 must beTrue

	    	// generated files must exist
	    	(out map { _.exists }).foldLeft(true) { (acc,fe) => acc && fe } must beTrue

	    	// generated files' size must be > 0 bytes
	    	(out map { _.length() }).foldLeft(true) { (acc,fs) => fs>0 && acc } must beTrue
	    }
	}
}
