
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

abstract class ResourcePlan[T,R](Version: Int, Group: String, Resource: String, MaxPageSize: Option[Int] = None) extends Plan with Logger {
	this: ResourceAuthComponent[T] =>
    	
	lazy val PathConfig = (Version, Group, Resource)
    lazy val PathPrefix = "/api/%s/%s/%s" format("v"+Version, Group, Resource)

    def permIfAuth(auth: Option[T]) : Boolean = ResourcePlan permIfAuth auth
    def toJson(obj: Any): String = ResourcePlan toJson obj
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = ResourcePlan fromJson json
    
    private def reject[X](auth: Option[T], req: HttpRequest[X], resp: ErrorResponse): ResponseFunction[Any] = auth match {
//        case Some(_) => Forbidden ~> resp // authenticated but unauthorized
//        case _ => WWWAuthenticate(Realm) ~> resp // might need authentication
        case _ => Forbidden ~> resp // don't use wwwauthenticate for httpbasic, to avoid popup
    }
    
    // authorization testers
    //	
    def authorizeSave[X](auth: Option[T], req: HttpRequest[X]): Boolean
    def authorizeUpdate[X](auth: Option[T], req: HttpRequest[X], id: String): Boolean
    def authorizeGetAll[X](auth: Option[T], req: HttpRequest[X]): Boolean
    def authorizeGet[X](auth: Option[T], req: HttpRequest[X], id: String): Boolean
    def authorizeDelete[X](auth: Option[T], req: HttpRequest[X], id: String): Boolean
    
    // resource handlers
    //
    def query[X](req: HttpRequest[X], page: Option[Int] = None, size: Option[Int] = None): List[R]
    def count[X](req: HttpRequest[X]) : Int
    def find(id: String): Option[R]
    def save(resource: R): Option[R]
    def update(original: R, resource: R): Option[R]
    def delete(resource: R): Boolean
    
    // resource converters
    //
    def deserialize[X](auth: Option[T], req: HttpRequest[X], data: String): Option[R]
    def serialize(auth: Option[T], resource: R): String = toJson(resource)
    def serialize(auth: Option[T], resources: List[R]): String = toJson(resources)
        
    private implicit def resourceToResponse(authAndResource: (Option[T],R)): ResponseFunction[Any] = {
        JsonContent ~> ResponseString(serialize(authAndResource._1, authAndResource._2))
    }
        
    private implicit def resourcesToResponse(authAndResource: (Option[T],List[R])): ResponseFunction[Any] = {
        JsonContent ~> ResponseString(serialize(authAndResource._1, authAndResource._2))
    }
        
    private implicit def intToResponse(authAndResource: (Option[T],Int)): ResponseFunction[Any] = {
        JsonContent ~> ResponseString("""{"count": %d}""" format authAndResource._2)
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

    def intent = {
        
        case req @ Path(p) if p.startsWith(PathPrefix) => req match {
            case Accepts.Json(_) => authorize(authService.authenticate(req))(req)
            case _ => NotAcceptable ~> ResponseString("You must accept application/json")
        }
        case _ => Pass
	}
    
    private def authorize(auth: Option[T]): Plan.Intent = {

	    // POST request must contain JSON
	    //
    	case req @ POST(Path(_) & RequestContentType("application/json")) => {
            if (!authorizeSave(auth, req)) reject(auth, req, ErrPostUnauthorized)
            else deserialize(auth, req, req) match {
	                case Some(toSave) => {
	                    save(toSave) match {
	                        case Some(saved) => Created ~> (auth,saved)
	                        case _ => ErrPostCannotCreate
	                    }
	                }
	                case _ => ErrPostCannotDeser
            	}
        }

    	// PUT request must contain JSON
    	//
        case req @ PUT(Path(Seg("api" :: v :: g :: r :: id :: Nil)) & RequestContentType("application/json")) => {
            if (!authorizeUpdate(auth, req, id)) reject(auth, req, ErrPutUnauthorized)
            else {
                find(id) match {
	                case Some(original) => {			                    
	                    deserialize(auth, req, req) match {
	                        case Some(toUpdate) => {
	                            update(original, toUpdate) match {
	                                case Some(updated) => Ok ~> (auth,updated)
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

        case req @ GET(Path(Seg("api" :: v :: g :: r :: id :: Nil))) => {
            if (!authorizeGet(auth, req, id)) reject(auth, req, ErrGetUnauthorized)
            else find(id) match {
                case Some(found) => Ok ~> (auth,found)
                case _ => ErrGetCannotFind
            }
        }

        case req @ GET(Path(p)) if p == PathPrefix => {
            if (!authorizeGetAll(auth, req)) reject(auth, req, ErrGetUnauthorized)
            object Count extends Params.Extract("count", Params.first ~> Params.nonempty)
            object Page extends Params.Extract("page", Params.first ~> Params.int)
            object Size extends Params.Extract("size", Params.first ~> Params.int)
            req match {
                case Params(Count(flag)) if (flag == "true" | flag == "TRUE") => Ok ~> (auth,count(req))
                case Params(Page(p) & Size(s)) => {
                    val sz = MaxPageSize match {
                        case Some(max) => if (max>s) s else max
                        case None => s
                    }
                    Ok ~> (auth,query(req, Some(p),Some(sz)))
                }
                case Params(Size(s)) => {
                    val sz = MaxPageSize match {
                        case Some(max) => if (max>s) s else max
                        case None => s
                    }
                    Ok ~> (auth,query(req, None,Some(sz)))
                }
                case _ => {
                	MaxPageSize match {
                        case Some(max) => Ok ~> (auth,query(req, None,Some(max)))
                        case None => Ok ~> (auth,query(req))
                    }
                }
            }
        }
            
        case req @ DELETE(Path(Seg("api" :: v :: g :: r :: id :: Nil))) => {
            if (!authorizeDelete(auth, req, id)) reject(auth, req, ErrDeleteUnauthorized)
            else find(id) match {
                case Some(toDelete) => {
                    if (delete(toDelete)) Ok
                    else ErrDeleteCannotDelete
                }
                case _ => ErrDeleteCannotFind
            }
        }

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
