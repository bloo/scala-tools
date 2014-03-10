
package b.uf.scalate

import unfiltered.request._
import unfiltered.response._
import java.io.OutputStreamWriter
import b.scalate._
import com.typesafe.config.ConfigFactory

object ScalateResponder {
	val cfg = ConfigFactory.load().getConfig("b.uf.scalate")
	val ext = cfg getString "ext"
	val location = cfg getString "location"
	val layout = cfg getString "layout"
    val engine: ScalateEngine = new ScalateEngine(ext, location, layout)
	engine setDebug(cfg getBoolean "debug")
}

trait ScalateResponder extends b.log.Logger {

    def render = ScalateResponder.engine.render _
    
    def global(attr: (String, Any)) = globals = globals.filter{ case (k,_) => k != attr._1 } :+ attr
    def prefix(pre: String) = pathPrefix = pre

    private var globals: List[(String, Any)] = Nil
    private var pathPrefix: String = ""

    def onTemplate(template: String) = {} // override in subclass

    def t[A](path: String, req: HttpRequest[A]): ResponseWriter =
        t(path, path.replaceAll("^/", "").replaceAll("/", " ").capitalize, req)

    def t[A](path: String, title: String, req: HttpRequest[A]): ResponseWriter =
        r(path, req, "title" -> title)

    def t[A](path: String, title: String, req: HttpRequest[A], attribute: (String, Any)): ResponseWriter =
        r(path, req, "title" -> title, attribute)

    def t[A](path: String, title: String, req: HttpRequest[A], attributes: (String, Any)*): ResponseWriter =
        r(path, req, (List("title" -> title) ::: attributes.toList): _*)

    def r[A](path: String, req: HttpRequest[A]): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, Nil: _*)

    def r[A](path: String, req: HttpRequest[A], attribute: (String, Any)): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, (attribute :: Nil): _*)

    def r[A](path: String, req: HttpRequest[A], attributes: (String, Any)*): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, attributes: _*)

    def respond[A](template: String, request: HttpRequest[A], attributes: (String, Any)*) = {
        onTemplate(template)
        new ResponseWriter {
            def write(writer: OutputStreamWriter) {
                render(template, writer, (attributes.toList ::: globals))
            }
        }
    }
}
