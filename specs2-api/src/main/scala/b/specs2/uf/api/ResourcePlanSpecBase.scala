package b.specs2.uf.api

import unfiltered.specs2.jetty.Served
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2._
import org.specs2.mock._
import b.common.Logger
import org.apache.commons.io.IOUtils
import java.io.StringWriter

trait ResourcePlanSpecBase[R, T, P <: b.uf.api.ResourcePlan[T,R]]
    extends Specification with Mockito with Served with Logger {

    type Requester = (String, String)
    def validUsers: List[Requester]
    def resourcePlan: P
    def pathPrefix: String = {
//        val cfg = pathConfig
        val cfg = resourcePlan.PathConfig
        "api/v%d/%s/%s" format (cfg._1, cfg._2, cfg._3)
    }

    // http://dispatch-classic.databinder.net/Try+Dispatch.html
    import dispatch.classic._
    import b.uf.api.ResourcePlan.fromJson

    // from: unfiltered.specs2.jetty.Served
    def setup = _.plan(resourcePlan)

    def newReq(id: String, requester: Option[Requester]): dispatch.classic.Request = newReq(Some(id), requester)
    def newReq(requester: Option[Requester]): dispatch.classic.Request = newReq(None, requester)
    def newReq(id: Option[String], requester: Option[Requester]): dispatch.classic.Request = {
        val path = id match {
            case Some(i) => pathPrefix + "/" + i
            case _ => pathPrefix
        }
        val req = host / path <:< Map("accept" -> "application/json", "content-type" -> "application/json")
        requester match {
            case Some((user, pass)) => req as_! (user, pass)
            case _ => req
        }
    }

    private def _reqToStatusAndResult(req: dispatch.classic.Request)(implicit mf: Manifest[R]): Handler[(Int,Option[R])] = {
        req >:> Predef.identity apply {
            case (status, _, Some(entity), _) if status == 200 | status == 201 => {
		    	val sw = new StringWriter
		    	IOUtils.copy(entity.getContent, sw)
		    	val jsonOut = sw.toString
		    	logger info "JSON ouput" + jsonOut
                (status, Some(fromJson[R](jsonOut)))
            }
            case (status, _, _, _) => (status, None)
        }
    }

    private def _reqToStatus(req: dispatch.classic.Request): Handler[Int] = {
	    req >:> Predef.identity apply { case (status,_,_,_) => status }
	}

    // http://blog.xebia.com/2011/11/26/easy-breezy-restful-service-testing-with-dispatch-in-scala/

    def xpost(json: String, id: String)(implicit mf: Manifest[R], requester: Option[Requester] = None)
    	= xhttp(_reqToStatusAndResult(newReq(requester) << (json)))
   	def post(json: String)(implicit mf: Manifest[R], requester: Option[Requester] = None): R = {
        val resultJson = http(newReq(requester) << (json) as_str)
        fromJson[R](resultJson)
    }

    def xget(id: String)(implicit mf: Manifest[R], requester: Option[Requester] = None): (Int,Option[R])
    	= xhttp(_reqToStatusAndResult(newReq(id, requester)))
    def get(id: String)(implicit mf: Manifest[R], requester: Option[Requester] = None): R = {
        val resultJson = http(newReq(id, requester) as_str)
        fromJson[R](resultJson)
    }

    def xdelete(id: String)(implicit requester: Option[Requester] = None) = xhttp(_reqToStatus(newReq(id, requester).DELETE))
    def delete(id: String)(implicit requester: Option[Requester] = None) = http(newReq(id, requester).DELETE as_str)

    def xput(json: String, id: String)(implicit mf: Manifest[R], requester: Option[Requester] = None)
    	= xhttp(_reqToStatusAndResult(newReq(id, requester) <<< (json)))
    def put(json: String, id: String)(implicit mf: Manifest[R], requester: Option[Requester] = None): R = {
        val resultJson = http(newReq(id, requester) <<< (json) as_str)
        fromJson[R](resultJson)
    }    
}
