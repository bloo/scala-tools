
package b.uf.api

import java.io.StringWriter
import java.util.UUID
import org.apache.commons.io.IOUtils
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.parse
import net.liftweb.json.pretty
import net.liftweb.json.render
import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response._
import unfiltered.response.ResponseString
import unfiltered.response.Unauthorized
import b.common.Logger

object ResourcePlan {
    
    var prettyJson = false
    
    // helper for subclasses - if authenticated, return true
    //
    def permIfAuth[A](auth: Option[A]) : Boolean = auth match {
        case Some(_) => true
        case _ => false
    }
    
    // helper json serializer
    //
    def toJson(obj: Any): String = {
    	import net.liftweb.json._
	    import net.liftweb.json.Extraction._
	    implicit val formats = DefaultFormats // brings in default date formats etc.
        val doc = render(decompose(obj))
        if (!prettyJson) pretty(doc) else compact(doc)
    }
    
    // helper json deserializer
    //
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = {
    	import net.liftweb.json._
	    import net.liftweb.json.Extraction._
	    implicit val formats = DefaultFormats // Brings in default date formats etc.
	    extract[O](parse(json))
    }
}

abstract class ResourcePlan[T,R](Version: Int, Group: String, ResourcePath: String, MaxPageSize: Option[Int] = None) extends Plan with Logger {
	this: ResourceAuthComponent[T] =>
    	
	lazy val PathConfig = (Version, Group, ResourcePath)
    lazy val PathPrefix = "/api/%s/%s/%s" format("v"+Version, Group, ResourcePath)

    def permIfAuth(auth: Option[T]) : Boolean = ResourcePlan permIfAuth auth
    def toJson(obj: Any): String = ResourcePlan toJson obj
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = ResourcePlan fromJson json
    
    private def reject(ctx: Context, resp: ErrorResponse): ResponseFunction[Any] = ctx.auth match {
//        case Some(_) => Forbidden ~> resp // authenticated but unauthorized
//        case _ => WWWAuthenticate(Realm) ~> resp // might need authentication
        case _ => Forbidden ~> resp // don't use wwwauthenticate for httpbasic, to avoid popup
    }
    
    // authorization testers
    //	
    def authorizeSave(ctx: Context): Boolean
    def authorizeUpdate(ctx: Context): Boolean
    def authorizeGetAll(ctx: Context): Boolean
    def authorizeGet(ctx: Context): Boolean
    def authorizeDelete(ctx: Context): Boolean
    
    // resource handlers
    //
    def query(ctx: Context, page: Option[Int] = None, size: Option[Int] = None): List[R]
    def count(ctx: Context) : Int
    def find(rid: String): Option[R]
    def save(resource: R): Option[R]
    def update(original: R, resource: R): Option[R]
    def delete(resource: R): Boolean
    
    // resource converters
    //
    def deserialize(ctx: Context, data: String): Option[R]
    def serialize(ctx: Context, resource: R): String = toJson(resource)
    def serialize(ctx: Context, resources: List[R]): String = toJson(resources)
        
    private implicit def resourceToResponse(ctxAndResource: (Context,R)): ResponseFunction[Any] = {
        JsonContent ~> ResponseString(serialize(ctxAndResource._1, ctxAndResource._2))
    }
        
    private implicit def resourcesToResponse(ctxAndResource: (Context,List[R])): ResponseFunction[Any] = {
        JsonContent ~> ResponseString(serialize(ctxAndResource._1, ctxAndResource._2))
    }
        
    private implicit def intToResponse(ctxAndResource: (Context,Int)): ResponseFunction[Any] = {
        JsonContent ~> ResponseString("""{"count": %d}""" format ctxAndResource._2)
    }
    
    private implicit def reqToResource[X](req: HttpRequest[X]): String = {
    	val sw = new StringWriter
    	IOUtils.copy(req.inputStream, sw)
    	sw.toString
    }

    private implicit def errorToJson(error: ErrorResponse): ResponseFunction[Any] = {
        val re = ResourceError(error.code, error.message)
        val json = toJson(re)
    	error.status ~> ResponseString(json)
    }

    private def dointent(req: HttpRequest[javax.servlet.http.HttpServletRequest], params: Map[String, String]) = {
        val ctx = Context(req, authService.authenticate(req), params, params.get("resource_id"))
        req match {
            case Accepts.Json(_) => authorize(ctx)(req)
            case _ => NotAcceptable ~> ResponseString("You must accept application/json")
        }
    }
    
    def intent = {
    	unfiltered.kit.Routes.specify(
   			PathPrefix -> dointent _,
   			PathPrefix+"/:resource_id" -> dointent _
    	)
    	
//        case req @ Path(p) if p.startsWith(PathPrefix) => req match {
//            case Accepts.Json(_) => authorize(authService.authenticate(req))(req)
//            case _ => NotAcceptable ~> ResponseString("You must accept application/json")
//        }
//        case _ => Pass
	}
    
    case class Context(req: HttpRequest[_], auth: Option[T], pathIds: Map[String,String], resourceId: Option[String])

    private def authorize(ctx: Context): Plan.Intent = {

    	ctx.resourceId match {
    	
    	    case Some(rid) => {
    	        
		    	// PUT request must contain JSON
		    	//
		        case req @ PUT(Path(_) & RequestContentType("application/json")) => {
		            if (!authorizeUpdate(ctx)) reject(ctx, ErrPutUnauthorized)
		            else {
		                find(rid) match {
			                case Some(original) => {			                    
			                    deserialize(ctx, req) match {
			                        case Some(toUpdate) => {
			                            update(original, toUpdate) match {
			                                case Some(updated) => Ok ~> (ctx,updated)
			                                case _ => ErrPutCannotUpdate
			                            }
			                        }
			                        case _ => ErrPutCannotDeser
			                    }
			                }
			                case _ => ErrPutCannotFind
		                }
		            }
		        }
		
		        // GET with id requests single resource
		        //
		        case req @ GET(Path(_)) => {
		            if (!authorizeGet(ctx)) reject(ctx, ErrGetUnauthorized)
		            else find(rid) match {
		                case Some(found) => Ok ~> (ctx,found)
		                case _ => ErrGetCannotFind
		            }
		        }    	     
		        
		        // DELETE resource by id
		        //
		        case req @ DELETE(Path(_)) => {
		            if (!authorizeDelete(ctx)) reject(ctx, ErrDeleteUnauthorized)
		            else find(rid) match {
		                case Some(toDelete) => {
		                    if (delete(toDelete)) Ok
		                    else ErrDeleteCannotDelete
		                }
		                case _ => ErrDeleteCannotFind
		            }
		        }
		        
		        // fall through
		        //
		        case _ => fail(ctx)
    	    }
    	    
    	    case None => {

			    // POST request must contain JSON
			    //
		    	case req @ POST(Path(_) & RequestContentType("application/json")) => {
		            if (!authorizeSave(ctx)) reject(ctx, ErrPostUnauthorized)
		            else deserialize(ctx, req) match {
			                case Some(toSave) => {
			                    save(toSave) match {
			                        case Some(saved) => Created ~> (ctx,saved)
			                        case _ => ErrPostCannotCreate
			                    }
			                }
			                case _ => ErrPostCannotDeser
		            	}
		        }
		    	
		    	// GET w/o id is a query
		    	//
		        case req @ GET(_) => {
		            if (!authorizeGetAll(ctx)) reject(ctx, ErrGetUnauthorized)
		            object Count extends Params.Extract("count", Params.first ~> Params.nonempty)
		            object Page extends Params.Extract("page", Params.first ~> Params.int)
		            object Size extends Params.Extract("size", Params.first ~> Params.int)
		            req match {
		                case Params(Count(flag)) if (flag == "true" | flag == "TRUE") => Ok ~> (ctx,count(ctx))
		                case Params(Page(p) & Size(s)) => {
		                    val sz = MaxPageSize match {
		                        case Some(max) => if (max>s) s else max
		                        case None => s
		                    }
		                    Ok ~> (ctx,query(ctx, Some(p),Some(sz)))
		                }
		                case Size(s) => {
		                    val sz = MaxPageSize match {
		                        case Some(max) => if (max>s) s else max
		                        case None => s
		                    }
		                    Ok ~> (ctx,query(ctx, None,Some(sz)))
		                }
		                case _ => {
		                	MaxPageSize match {
		                        case Some(max) => Ok ~> (ctx,query(ctx, None,Some(max)))
		                        case None => Ok ~> (ctx,query(ctx))
		                    }
		                }
		            }
		        } 
	
		        // fall through
		        //
		        case _ => fail(ctx)
    	    }
    	}
    }
    
    private def fail(ctx: Context): ErrorResponse = ctx.req match {

        // determine correct errors by process of elimination
        //
        case PUT(_) & RequestContentType("application/json") => ErrPutMissingId
        case PUT(_) => ErrPutNotJson
    	case POST(_) => ErrPostNotJson
        case GET(_) => ErrGetMissingId
        case DELETE(_) => ErrDeleteMissingId

        // exhausted the Method matchers!
        //
    	case req @ _ => ErrBadMethod(req.method)
    }
}

sealed abstract class ErrorBase
case class ErrorResponse(status: Status, code: Int, message: String) extends ErrorBase
case class ResourceError(val code: Int, val message: String)

object ErrPostUnauthorized extends ErrorResponse(Unauthorized, 1000, "unauthorized")
object ErrPostCannotCreate extends ErrorResponse(BadRequest, 1001, "unable to create resource")
object ErrPostCannotDeser extends ErrorResponse(BadRequest, 1002, "unable to deserialize resource from content")
object ErrPostNotJson extends ErrorResponse(UnsupportedMediaType, 1003, "content-type must be application/json")

object ErrPutUnauthorized extends ErrorResponse(Unauthorized, 2000, "unauthorized")
object ErrPutCannotFind extends ErrorResponse(NotFound, 2001, "unknown id")
object ErrPutCannotUpdate extends ErrorResponse(BadRequest, 2002, "unable to update resource")
object ErrPutCannotDeser extends ErrorResponse(BadRequest, 2003, "unable to deserialize resource from content")
object ErrPutMissingId extends ErrorResponse(NotFound, 2004, "resource id required in path")
object ErrPutNotJson extends ErrorResponse(UnsupportedMediaType, 2005, "content-type must be application/json")

object ErrGetUnauthorized extends ErrorResponse(Unauthorized, 3000, "unauthorized")
object ErrGetCannotFind extends ErrorResponse(NotFound, 3001, "unknown id")
object ErrGetMissingId extends ErrorResponse(NotFound, 3002, "resource id required in path")

object ErrDeleteUnauthorized extends ErrorResponse(Unauthorized, 4000, "unauthorized")
object ErrDeleteCannotFind extends ErrorResponse(NotFound, 4001, "unknown id")
object ErrDeleteCannotDelete extends ErrorResponse(BadRequest, 4002, "unable to delete resource")
object ErrDeleteMissingId extends ErrorResponse(NotFound, 4003, "resource id required in path")

class ErrBadMethod(val err: String) extends ErrorResponse(MethodNotAllowed, 9999, err)
object ErrBadMethod { def apply(method: String) = new ErrBadMethod("%s not supported" format method) }
