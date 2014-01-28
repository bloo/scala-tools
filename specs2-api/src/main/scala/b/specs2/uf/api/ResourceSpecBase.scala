package b.specs2.uf.api

import unfiltered.specs2.jetty.Served
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2._
import org.specs2.mock._
import b.log.Logger
import java.io.StringWriter

trait ResourceSpecBase[T, R, RR <: b.uf.api.Resource[T,R]]
    extends Specification with Mockito with Served with Logger {

    // tuple type representing the user/pass credentials of whomever 
    // is requesting each http client call during the specs test
    //
    type Requester = (String, String)

    // needs to be overridden in spec to provide actual instance of ResourcePlan
    //
    def resource: RR
    val group: Option[String] = None
    val version: Double = 1.0
    
    // get our path prefix from the ResourcePlan instance
    //
    private var _pathPrefix: Option[String] = None
    def pathPrefix: String = _pathPrefix.get

    // http://dispatch-classic.databinder.net/Try+Dispatch.html
    import dispatch.classic._

    // from: unfiltered.specs2.jetty.Served
    override def setup = { server =>
    	val res = resource
    	val plan = res.plan(group, version)
    	_pathPrefix = Some(res.FullPath.replaceFirst("/", ""))        
        server.plan(plan)        
    } 

    def newReq(id: String, requester: Option[Requester], params: Seq[(String,String)]): Request =
        newReq(Some(id), requester, params)
        
    def newReq(requester: Option[Requester], params: Seq[(String,String)]): Request =
        newReq(None, requester, params)

    def newReq(id: Option[String], requester: Option[Requester], params: Seq[(String,String)]): Request = {
        val path = id match {
            case Some(i) => pathPrefix + "/" + i
            case _ => pathPrefix
        }
        var req = host / path <:< Map("accept" -> "application/json", "content-type" -> "application/json")
        params foreach { p => req <<? Map(p) }
        requester match {
            case Some((user, pass)) => req as_! (user, pass)
            case _ => req
        }
    }

    private def _reqToStatusAndResult(req: dispatch.classic.Request)(implicit mf: Manifest[R]): Handler[(Int,Option[R])] = {
        req >:> Predef.identity apply {
            case (status, _, Some(entity), _) if status == 200 || status == 201 => {
		    	val jsonOut = _entityToJson(entity)
                (status, Some(fromJson[R](jsonOut)))
            }
            case (status, _, _, _) => (status, None)
        }
    }
    
    private def _reqToStatusAndResults(req: dispatch.classic.Request)(implicit mf: Manifest[R]): Handler[(Int,Option[List[R]])] = {
        req >:> Predef.identity apply {
            case (status, _, Some(entity), _) if status == 200 || status == 201 => {
		    	val jsonOut = _entityToJson(entity)
                (status, Some(fromJson[List[R]](jsonOut)))
            }
            case (status, _, _, _) => (status, None)
        }
    }

    private def _entityToJson(entity: org.apache.http.HttpEntity): String = {
    	val out = new StringWriter
    	val in = entity.getContent
    	import org.apache.commons.io.IOUtils
    	IOUtils copy(in, out)
    	IOUtils closeQuietly in
    	val json = out.toString
    	IOUtils closeQuietly out
    	json
    }
    
    private def _reqToStatus(req: dispatch.classic.Request): Handler[Int] =
	    req >:> Predef.identity apply { case (status,_,_,_) => status }

    // http://blog.xebia.com/2011/11/26/easy-breezy-restful-service-testing-with-dispatch-in-scala/

	def _params(req: Request, params: Seq[(String,String)]) = {
	    var r = req
	    params foreach { p => r = r <<? Map(p) }
	    r
	}
	    
    def xpost(json: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester] = None) =
    	xhttp(_reqToStatusAndResult(newReq(requester, params) << (json)))
   	def post(json: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): R = {
        val resultJson = http(newReq(requester, params) << (json) as_str)
        fromJson[R](resultJson)
    }

    def xget(id: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): (Int,Option[R]) =
    	xhttp(_reqToStatusAndResult(newReq(id, requester, params)))
    def get(id: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): R = {
        val resultJson = http(newReq(id, requester, params) as_str)
        fromJson[R](resultJson)
    }

    def xget(params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): (Int,Option[List[R]]) =
        xhttp(_reqToStatusAndResults(newReq(requester, params)))
    def get(params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): List[R] = {
        val resultJson = http(newReq(requester, params) as_str)
        fromJson[List[R]](resultJson)
    }

    def xdelete(id: String, params:(String,String)*)(implicit requester: Option[Requester]) =
        xhttp(_reqToStatus(newReq(id, requester, params).DELETE))
    def delete(id: String, params:(String,String)*)(implicit requester: Option[Requester]) =
        http(newReq(id, requester, params).DELETE as_str)

    def xput(json: String, id: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester])
    	= xhttp(_reqToStatusAndResult(newReq(id, requester, params) <<< (json)))
    def put(json: String, id: String, params:(String,String)*)(implicit mf: Manifest[R], requester: Option[Requester]): R = {
        val resultJson = http(newReq(id, requester, params) <<< (json) as_str)
        fromJson[R](resultJson)
    }    
    
    private def fromJson[O](json: String)(implicit mf: Manifest[O]): O = {
        import net.liftweb.json._
        import net.liftweb.json.Extraction._
        implicit val formats = DefaultFormats + b.uf.api.BigDecimalSerializer // Brings in default date formats etc.
        extract[O](parse(json))
    }
}
