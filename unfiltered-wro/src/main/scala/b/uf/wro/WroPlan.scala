
package b.uf.wro

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.Validate
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import ro.isdc.wro.config.jmx.WroConfiguration
import ro.isdc.wro.extensions.model.factory.SmartWroModelFactory
import ro.isdc.wro.extensions.processor.css.BourbonCssProcessor
import ro.isdc.wro.extensions.processor.js.CoffeeScriptProcessor
import ro.isdc.wro.extensions.processor.support.sass.RubySassEngine
import ro.isdc.wro.http.WroFilter
import ro.isdc.wro.manager.factory.ConfigurableWroManagerFactory
import ro.isdc.wro.model.resource.processor.factory.ProcessorsFactory
import ro.isdc.wro.model.resource.processor.factory.SimpleProcessorsFactory
import ro.isdc.wro.model.resource.processor.impl.css.CssImportPreProcessor
import ro.isdc.wro.model.resource.processor.impl.css.CssUrlRewritingProcessor
import ro.isdc.wro.model.resource.processor.impl.css.CssVariablesProcessor
import ro.isdc.wro.model.resource.processor.impl.css.JawrCssMinifierProcessor
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor
import ro.isdc.wro.model.resource.processor.impl.js.SemicolonAppenderPreProcessor
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor
import ro.isdc.wro.model.resource.processor.ResourcePostProcessor
import ro.isdc.wro.util.StopWatch
import b.log.Logger

object Wro {
    var gzip = true // false
    var debug = false
    var cssMin = false
    var jsMin = false
    
    /**
     * order here matters..
     * classes and traits are constructed super- to sub-class, left to right
     */
    
    // aggregate, pre- and post-process and minify CSS
    def libCssPlan(wroXml: String) = new WroPlan(wroXml) with CssPre with CssMinPost plan
    
    // aggregate, pre- and post-process and minify JS
    def libJsPlan(wroXml: String) = new WroPlan(wroXml) with JsPre with JsMinPost plan
    
    // aggregate, pre- and post-process and minify JS *AND* CSS resources
    def libAppPlan(wroXml: String) = new WroPlan(wroXml) with CssPre with CssMinPost with JsPre with JsMinPost plan
    
    // process CoffeeScript resources into JS
    def coffeePlan(wroXml: String) = new WroPlan(wroXml) with CoffeeScript with JsMinPost plan

    // process SCSS resources into CSS
    def scssPlan(wroXml: String) = new WroPlan(wroXml) with Scss with CssMinPost plan
    
    // process SASS resources into CSS
    def sassPlan(wroXml: String) = new WroPlan(wroXml) with Sass with CssMinPost plan
    
    // "application" plan that processes and minifies CoffeeScript and SASS
    def appPlan(wroXml: String) = new WroPlan(wroXml) with CoffeeScript with Sass with JsMinPost with CssMinPost plan

    // aggregate js and css only - no pre or post processing
    def aggOnlyPlan(wroXml: String) = new WroPlan(wroXml) plan
}

class WroPlan(file: String) extends Logger {

    logger info ("Building WRO plan for: %s" format file)
    
    private var pre = scala.collection.mutable.LinkedList[ResourcePreProcessor]()
    private var post = scala.collection.mutable.LinkedList[ResourcePostProcessor]()
	
    def addpre[T<:ResourcePreProcessor](processor: T) = pre = pre :+ processor
    def addpost[T<:ResourcePostProcessor](processor: T) = post = post :+ processor
    
	def plan: WroFilter = new WroFilter {

        Wro.jsMin = !Wro.debug
	    Wro.cssMin = !Wro.debug
	
	    val wroConfig = new WroConfiguration
	    wroConfig setDebug Wro.debug
	    wroConfig setDisableCache Wro.debug
	    //wroConfig.setModelUpdatePeriod(if (Wro.debug) 10 else 0)
	    wroConfig setGzipEnabled Wro.gzip
	    wroConfig setJmxEnabled false
	    wroConfig setIgnoreMissingResources false
	    setConfiguration(wroConfig)

	    val spf = new SimpleProcessorsFactory {
	        logger info ("[%d] pre-processors for %s" format(pre.size, file))
	        logger info ("[%d] post-processors for %s" format(post.size, file))
	        for (p <- pre) addPreProcessor(p)
	        for (p <- post) addPostProcessor(p)
	    }
	
        //ro.isdc.wro.maven.plugin.manager.factory.ConfigurableWroManagerFactory
	    val factory = new ConfigurableWroManagerFactory {
	
	        setProcessorsFactory(spf)
	
	        override def newModelFactory = {
	            new SmartWroModelFactory {
	            	setAutoDetectWroFile(false)
	                setWroFile(new File(file))
	            }
	        }
	    }
	    
	    setWroManagerFactory(factory)
    }
}

trait CoffeeScript { this: WroPlan =>
    addpost(new CoffeeScriptProcessor)
}

trait Sass extends CssUrlPre { this: WroPlan =>
    addpost(new BourbonCssProcessor {
        override def newEngine = new SmarterRubySassEngine("sass")
    })
}

trait Scss extends CssUrlPre { this: WroPlan =>
    addpost(new BourbonCssProcessor {
        override def newEngine = new SmarterRubySassEngine("scss")
    })
}

trait CssUrlPre { this: WroPlan =>
    addpre(new CssImportPreProcessor)
//    addpre(new CssUrlRewritingProcessor)
}

trait CssPre extends CssUrlPre { this: WroPlan =>
    addpre(new CssVariablesProcessor)
}

trait CssMinPost { this: WroPlan => 
    if (Wro.cssMin) addpost(new JawrCssMinifierProcessor)
}

// keeps throwing:
//ro.isdc.wro.WroRuntimeException: Compilation has errors: [JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 57 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 245 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 5066 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 8373 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10128 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10129 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10131 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10132 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10148 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 10296 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 11588 : 0, JSC_PARSE_ERROR. Parse error. identifier is a reserved word at libs line 11605 : 0]
//trait JsGCMinPost { this: WroPlan =>
//    if (Wro.jsMin) {
//		  import ro.isdc.wro.extensions.processor.js.GoogleClosureCompressorProcessor
//        val m = new GoogleClosureCompressorProcessor
//        m setEncoding "UTF8"
//        addpost(m)
//    }
//}

trait JsMinPost { this: WroPlan =>
    if (Wro.jsMin) addpost(new JSMinProcessor)
}

trait JsPre { this: WroPlan =>
    addpre(new SemicolonAppenderPreProcessor)
}

class SmarterRubySassEngine(syntax: String) extends RubySassEngine with Logger {

    val requires = List("rubygems", "sass/plugin", "sass/engine", "bourbon")

    override def process(content: String): String = {
        if (StringUtils.isEmpty(content)) StringUtils.EMPTY
        else {
            val stopWatch = new StopWatch()

            try {
                logger.debug("process " + syntax)
                stopWatch.start("process " + syntax)
                val rubyEngine: ScriptEngine = new ScriptEngineManager().getEngineByName("jruby")
                val updateScript = buildUpdateScript(content, syntax)
                val css = rubyEngine.eval(updateScript).toString();
                css
            } finally {
                stopWatch.stop();
                logger.debug(stopWatch.prettyPrint());
            }
        }
    }

    private def buildUpdateScript(content: String, syntax: String): String = {

        Validate.notNull(content)
        val raw = new StringWriter()
        val script = new PrintWriter(raw)
        val sb = new StringBuilder()
        sb.append(":syntax => :" + syntax)

        for (require <- requires) script.println("  require '" + require + "'                                   ");

        val scriptAsString = String.format("result = Sass::Engine.new('%s', {%s}).render",
            content.replace("'", "\""), sb.toString())
        logger.debug("scriptAsString: " + scriptAsString)
        script.println(scriptAsString)
        script.flush()
        raw.toString()
    }
}

