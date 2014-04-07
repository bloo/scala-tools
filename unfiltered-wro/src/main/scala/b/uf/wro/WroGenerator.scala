package b.uf.wro

import ro.isdc.wro.http.WroFilter
import java.io.File
import java.util.UUID
import ro.isdc.wro.manager.factory.standalone.DefaultStandaloneContextAwareManagerFactory

object WroGenerator extends b.log.Logger {

	def start(routes: (String, WroFilter)*): (unfiltered.jetty.Http, Seq[String]) = {

		val generator = unfiltered.jetty.Http(unfiltered.util.Port.any)
		WroPlans.planner(generator, routes:_*)

		import javax.servlet.FilterConfig
		import javax.servlet.http.HttpServletRequest
		import javax.servlet.http.HttpServletResponse

		import org.mockito.Mockito

		import b.uf.wro.WroPlans
		import ro.isdc.wro.config.Context
		import ro.isdc.wro.config.jmx.WroConfiguration
		import ro.isdc.wro.http.WroFilter
		import ro.isdc.wro.model.resource.ResourceType
		import scala.collection.JavaConversions._

		val paths: Seq[String] = routes flatMap {
			case (ctx, plan) => {
				// mocks to set wro context.. god i hate this library
				val request = Mockito.mock(classOf[HttpServletRequest])
				val response = Mockito.mock(classOf[HttpServletResponse])
				val fConfig = Mockito.mock(classOf[FilterConfig])
				val config = new WroConfiguration()
				Context.set(Context.webContext(request, response, fConfig), config)

				plan.getWroManagerFactory().create().getModelFactory().create().getGroups().toSeq flatMap { group =>
					ResourceType.values().toSeq map { ext => ctx + "/" + group.getName() + "." + ext.name().toLowerCase() }
				}
			}
		}

		val rp = routes map { case (p,_) => p }
		generator start ()
		generator -> paths
	}

	def generate(outputDir: File, routes: (String, WroFilter)*): Seq[File] = {
		val (generator, paths) = start(routes: _*)
		val port = generator.port
		info("WroGenerator started.")
		val dumped = dump(port, paths, outputDir)
		info("WroGenerator completed. stopping...")
		stop(generator)		
		info("WroGenerator stopped.")
		dumped
	}
	
	def stop(generator: unfiltered.jetty.Http) = {
		generator stop()
		generator destroy()
	}
	
	def dump(port: Int, paths: Seq[String], outputDir: File): Seq[File] = {
		val syncList = new scala.collection.mutable.ArrayBuffer[File]() with scala.collection.mutable.SynchronizedBuffer[File]

		import scala.concurrent._
		import scala.concurrent.duration._
		import ExecutionContext.Implicits.global
		import scala.util.{ Success, Failure }
		import dispatch.classic._
		
		info(s"Started WroGenerator @ localhost:" + port)

		val h = new Http with thread.Safety
		val futures = paths map { path =>
			val f = future {
				info(s"Requesting path $path")
				// http://dispatch-classic.databinder.net/Try+Dispatch.html
				val req = :/("localhost", port) / path.replaceFirst("/", "")
				val p = req.path
				h(req as_str)
			}
			f onComplete {
				case Success(content) => syncList += _entityToFile(content, outputDir + path)
				case Failure(t) => throw t
			}
			f
		}
		futures foreach { Await.ready(_, 5 minutes) }
		h.shutdown
		syncList.toSeq
	}

	private def _entityToFile(content: String, path: String): File = {
		val file = new File(path)
		info(s"write to file: $file")
		val dir = file.getParent()
		info(s"making dir: $dir")
		org.apache.commons.io.FileUtils.forceMkdir(new java.io.File(dir))
		org.apache.commons.io.FileUtils.write(file, content)
		file
	}
}