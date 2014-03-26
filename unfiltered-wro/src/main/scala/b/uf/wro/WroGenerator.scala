package b.uf.wro

import ro.isdc.wro.http.WroFilter
import java.io.File

object WroGenerator extends b.log.Logger {

	def generate(outputDir: String, resources: (String, WroFilter)*): Seq[File] =
		generate(new File(outputDir), resources:_*)

	def generate(outputDir: File, resources: (String, WroFilter)*): Seq[File] = {

		val generator = unfiltered.jetty.Http(unfiltered.util.Port.any)
		val paths = WroPlans.planner(generator, resources:_*)

		generator.start()
		info("started.")
		val genedFiles = visitPaths(outputDir, paths, generator)
		info("completed. stopping...")
		generator stop()
		generator destroy()
		info("stopped.")
		genedFiles
	}

	def visitPaths(dest: File, paths: Seq[String], generator: unfiltered.jetty.Http): Seq[File] = {

		val syncList = new scala.collection.mutable.ArrayBuffer[File]()
			with scala.collection.mutable.SynchronizedBuffer[File]

		import scala.concurrent._
		import scala.concurrent.duration._
		import ExecutionContext.Implicits.global
		import scala.util.{ Success, Failure }
		import dispatch.classic._
		
		info(s"Started server localhost:" + generator.port)
		val h = new Http with thread.Safety
		val futures = paths map { path =>
			val f = future {
				info(s"Requesting path $path")
				// http://dispatch-classic.databinder.net/Try+Dispatch.html
				val req = :/("localhost", generator.port) / path.replaceFirst("/", "")
				h(req as_str)
			}
			f onComplete {
				case Success(content) => syncList += _entityToFile(content, dest + path)
				case Failure(t) => throw t
			}
			f
		}
		futures foreach { Await.ready(_, 5 minutes) }
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