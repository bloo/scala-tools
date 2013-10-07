
package b.uf.scalate

import unfiltered.request._
import unfiltered.response._

import org.fusesource.scalate.layout.DefaultLayoutStrategy

import java.io.PrintWriter

import org.fusesource.scalate.DefaultRenderContext
import org.fusesource.scalate.TemplateEngine

import java.io.File

import org.fusesource.scalate.RenderContext

import java.io.OutputStreamWriter

import org.fusesource.scalate.Binding

object Scalate {
    var templateExt = ".jade"
    var templatePath = "templates/html/";
    var layoutTemplate = "layouts/default.jade"
    var debug = false
}

trait Scalate {
    
    def global(attr: (String,Any)) = globals = globals :+ attr
    def prefix(pre: String) = pathPrefix = pre
    
    private var globals: List[(String,Any)] = Nil
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
        ScalateTemplate(pathPrefix + "/" + path.replaceAll("^/", "") + Scalate.templateExt) respond (req, globals: _*)

    def r[A](path: String, req: HttpRequest[A], attribute: (String, Any)): ResponseWriter =
        ScalateTemplate(pathPrefix + "/" + path.replaceAll("^/", "") + Scalate.templateExt) respond (req, (attribute :: globals): _*)

    def r[A](path: String, req: HttpRequest[A], attributes: (String, Any)*): ResponseWriter =
        ScalateTemplate(pathPrefix + "/" + path.replaceAll("^/", "") + Scalate.templateExt) respond (req, (attributes.toList ::: globals): _*)

    //    def engine = ScalateTemplate.engine
    //    def contextBuilder = ScalateTemplate.renderContext
}

object ScalateTemplate {

    def apply(template: String) = new ScalateTemplate(Scalate.templatePath + template)

    /* Function to construct a RenderContext. */
    type ToRenderContext = (String, PrintWriter, TemplateEngine) => RenderContext

    val engine = new TemplateEngine
    engine.layoutStrategy = new DefaultLayoutStrategy(engine, Scalate.templatePath + Scalate.layoutTemplate)
    engine.allowCaching = !Scalate.debug
    engine.allowReload = Scalate.debug
    engine.mode = if (Scalate.debug) "development" else "production"
    val renderContext: ToRenderContext = (path, writer, engine) => new DefaultRenderContext(path, engine, writer)
}

class ScalateTemplate(val template: String) extends b.common.Logger {

    /**
     * Constructs a ResponseWriter for Scalate templates.
     *  Note that any parameter in the second, implicit set
     *  can be overriden by specifying an implicit value of the
     *  expected type in a particular scope.
     */
    def respond[A](request: HttpRequest[A], attributes: (String, Any)*)(
        implicit engine: TemplateEngine = ScalateTemplate.engine,
        contextBuilder: ScalateTemplate.ToRenderContext = ScalateTemplate.renderContext,
        bindings: List[Binding] = Nil,
        additionalAttributes: Seq[(String, Any)] = Nil) = new ResponseWriter {
        def write(writer: OutputStreamWriter) {
            val printWriter = new PrintWriter(writer)
            try {
                val context = contextBuilder(Path(request), printWriter, engine)
                (additionalAttributes ++ attributes) foreach {
                    case (k, v) => context.attributes(k) = v
                }
                engine.layout(template, context)
            } catch {
                case e: Throwable => {
                    logger error ("Unable to render Scalate template for request", e)
                    throw e
                }
            }
        }
    }
}