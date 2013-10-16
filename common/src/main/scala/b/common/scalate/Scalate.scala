package b.common.scalate

import org.fusesource.scalate.layout.DefaultLayoutStrategy
import org.fusesource.scalate.DefaultRenderContext
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.RenderContext
import org.fusesource.scalate.Binding
import java.io.Writer
import java.io.PrintWriter
import java.io.File


object ScalateEngine {
    type ToRenderContext = (String, PrintWriter, TemplateEngine) => RenderContext
}

class ScalateEngine(templateExt: String, templatePath: String, layoutTemplate: String) extends b.common.Logger {
    private val engine = new TemplateEngine
    engine.layoutStrategy = new DefaultLayoutStrategy(engine, templatePath + "/" + layoutTemplate)
    private val renderContext: ScalateEngine.ToRenderContext = (path, writer, engine) => new DefaultRenderContext(path, engine, writer)

    def setDebug(debug: Boolean) = {
        engine.allowCaching = !debug
        engine.allowReload = debug
        engine.mode = if (debug) "development" else "production"
    }
    
    def render(template: String, writer: Writer, attributes: (String, Any)*) = {
    	val path = if (template.endsWith(templateExt)) template else template + templateExt
		val tmpl = templatePath + "/" + path
    	try {
    		val context = renderContext(path, new PrintWriter(writer), engine)
    		attributes foreach { case (k, v) => context.attributes(k) = v }
    		engine.layout(tmpl, context)
        } catch {
            case e: Throwable => {
            	logger error ("Unable to render Scalate template for request", e)
                throw e
            }
        }
    }
}

