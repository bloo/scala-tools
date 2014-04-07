package b.specs2.wro

import org.junit.runner.RunWith
import org.specs2._
import org.specs2.mock._
import org.specs2.mutable.Specification
import b.log.Logger
import org.specs2.execute.AsResult
import org.specs2.specification.AroundOutside
import java.io.File
import org.specs2.execute.Result
import java.util.UUID
import b.uf.wro.WroGenerator
import ro.isdc.wro.http.WroFilter
import unfiltered.jetty.Http
import java.net.URL

abstract class WroSpecBase extends Specification with Mockito with Logger {

	def outputDir = {
		val outputDir = File.createTempFile("wro-gen-test", UUID.randomUUID().toString)
		outputDir.delete
		outputDir.mkdir
		outputDir
	}
	
	def file(path: String) = new File(path)
	
	type Gen = Seq[(String,WroFilter)]=>Seq[File]
    object wro extends AroundOutside[Gen] {
        var res: Option[Gen] = None
    	def around[R : AsResult](a: =>R): Result = {
    		val r: Gen = { routes: Seq[(String,WroFilter)] =>
				val parent = getClass getClassLoader
				val cl = new java.net.URLClassLoader(Array(new URL("file://src/test/resources")), parent)
    			WroGenerator.generate(outputDir, routes:_*)
    		}
    		res = Some(r)
        	AsResult(a)
        }
    	def outside: Gen = res.get
    }


}
