
package b.uf.scalate

import unfiltered.request._
import unfiltered.response._
import java.io.OutputStreamWriter
import b.common.scalate._

object Scalate {
    private var cfg: ScalateEngine = null
    def config(debug: Boolean = false, cfg: ScalateEngine = new ScalateEngine(".jade", "templates/html", "layouts/default.jade")) = {
        cfg setDebug debug
        Scalate.cfg = cfg
    }
}

trait Scalate extends b.common.Logger {

    def global(attr: (String, Any)) = globals = globals :+ attr
    def prefix(pre: String) = pathPrefix = pre

    private var globals: List[(String, Any)] = Nil
    private var pathPrefix: String = ""

    def t[A](path: String, req: HttpRequest[A]): ResponseWriter =
        t(path, path.replaceAll("/", " ").capitalize, req)

    def t[A](path: String, title: String, req: HttpRequest[A]): ResponseWriter =
        r(path, req, "title" -> title)

    def t[A](path: String, title: String, req: HttpRequest[A], attribute: (String, Any)): ResponseWriter =
        r(path, req, "title" -> title, attribute)

    def t[A](path: String, title: String, req: HttpRequest[A], attributes: (String, Any)*): ResponseWriter =
        r(path, req, (List("title" -> title) ::: attributes.toList): _*)

    def r[A](path: String, req: HttpRequest[A]): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, globals: _*)

    def r[A](path: String, req: HttpRequest[A], attribute: (String, Any)): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, (attribute :: globals): _*)

    def r[A](path: String, req: HttpRequest[A], attributes: (String, Any)*): ResponseWriter =
        respond(pathPrefix + "/" + path.replaceAll("^/", ""), req, (attributes.toList ::: globals): _*)

    def respond[A](template: String, request: HttpRequest[A], attributes: (String, Any)*) = new ResponseWriter {
        def write(writer: OutputStreamWriter) {
            val path = Path(request)
            Scalate.cfg.render(path, template, writer)
        }
    }
}
