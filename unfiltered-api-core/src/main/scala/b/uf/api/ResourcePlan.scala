
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
import b.uf.errors._

case class Context[T](req: HttpRequest[_], auth: Option[T], pathIds: Map[String, String]) {
    def hasAuth = !auth.isEmpty
}
case class QueryParams(page: Option[Int] = None, size: Option[Int])

object ResourcePlan {

    var prettyJson = false

    // helper json serializer
    //
    def toJson(obj: Any): String = {
        import net.liftweb.json._
        import net.liftweb.json.Extraction._
        implicit val formats = DefaultFormats + BigDecimalSerializer // brings in default date formats etc.
        val doc = render(decompose(obj))
        if (!prettyJson) pretty(doc) else compact(doc)
    }

    // helper json deserializer
    //
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = {
        import net.liftweb.json._
        import net.liftweb.json.Extraction._
        implicit val formats = DefaultFormats + BigDecimalSerializer // Brings in default date formats etc.
        extract[O](parse(json))
    }
}

abstract class ResourcePlan[T, R](Version: Int, Group: String, ResourcePath: String, MaxPageSize: Option[Int] = None) extends Plan with Logger {
    this: ResourceAuthComponent[T] =>

    lazy val PathConfig = (Version, Group, ResourcePath)
    lazy val PathPrefix = "/api/%s/%s/%s" format ("v" + Version, Group, ResourcePath)
    private val ResourceIdParam = "resource_id"

    def toJson(obj: Any): String = ResourcePlan toJson obj
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = ResourcePlan fromJson json

    private def reject(ctx: Context[T], resp: ErrorResponse): ResponseFunction[Any] = ctx.auth match {
        //        case Some(_) => Forbidden ~> resp // authenticated but unauthorized
        //        case _ => WWWAuthenticate(Realm) ~> resp // might need authentication
        case _ => Forbidden ~> resp // don't use wwwauthenticate for httpbasic, to avoid popup
    }

    // subclass needs to implement resource resolver
    //
    def resolve(resourceId: String): Option[R]

    type Creator = PartialFunction[Context[T], R => Option[R]]
    private var _creator: Creator = Map.empty
    def create(c: Creator) = _creator = c

    type RawCreator = PartialFunction[Context[T], String => Option[R]]
    private var _rawCreator: RawCreator = Map.empty
    def rcreate(c: RawCreator) = _rawCreator = c

    type Getter = PartialFunction[Context[T], String => Option[R]]
    private var _getter: Getter = Map.empty
    def get(g: Getter) = _getter = g

    type Querier = PartialFunction[Context[T], QueryParams => List[R]]
    private var _querier: Querier = Map.empty
    def query(q: Querier) = _querier = q

    type Counter = PartialFunction[Context[T], () => Int]
    private var _counter: Counter = Map.empty
    def count(c: Counter) = _counter = c

    type Updater = PartialFunction[Context[T], (R, R) => Option[R]]
    private var _updater: Updater = Map.empty
    def update(u: Updater) = _updater = u

    type Deleter = PartialFunction[Context[T], R => Boolean]
    private var _deleter: Deleter = Map.empty
    def delete(d: Deleter) = _deleter = d

    // resource converters
    //
    def deserialize(ctx: Context[T], data: String): R = fromJson(data)
    def serialize(ctx: Context[T], resource: R): String = toJson(resource)
    def serialize(ctx: Context[T], resources: List[R]): String = toJson(resources)

    private implicit def resourceToResponse(cr: (Context[T], R)): ResponseFunction[Any] =
        JsonContent ~> ResponseString(serialize(cr._1, cr._2))

    private implicit def resourcesToResponse(cr: (Context[T], List[R])): ResponseFunction[Any] =
        JsonContent ~> ResponseString(serialize(cr._1, cr._2))

    private implicit def intToResponse(cr: (Context[T], Int)): ResponseFunction[Any] =
        JsonContent ~> ResponseString("""{"count": %d}""" format cr._2)

    private implicit def reqToResource[X](req: HttpRequest[X]): String = {
        val sw = new StringWriter
        IOUtils.copy(req.inputStream, sw)
        sw.toString
    }

    case class ResourceErrorJson(val code: Int, val messages: Seq[String])
    private implicit def errorToJson(error: ErrorResponse): ResponseFunction[Any] = {
        val re = ResourceErrorJson(error.code, error.messages)
        val json = toJson(re)
        error.status ~> ResponseString(json)
    }

    def intent = {
        unfiltered.kit.Routes.specify(
            PathPrefix -> dointent _,
            PathPrefix + ("/:%s" format ResourceIdParam) -> dointent _)
    }

    private def dointent(req: HttpRequest[javax.servlet.http.HttpServletRequest], params: Map[String, String]) = {
        val ctx = Context[T](req, authService.authenticate(req), params)
        req match {
            case Accepts.Json(_) => authorize(ctx, params.get(ResourceIdParam))(req)
            case _ => NotAcceptable ~> ResponseString("You must accept application/json")
        }
    }

    private def authorize(ctx: Context[T], resourceId: Option[String]): Plan.Intent = resourceId match {
        case Some(rid) => {

            // PUT request must contain JSON
            //
            case req @ PUT(Path(_) & RequestContentType("application/json")) => {
                _updater lift ctx match {
                    case None => reject(ctx, ErrPutUnauthorized)
                    case Some(u) => resolve(rid) match {
                        case None => ErrPutCannotResolveId
                        case Some(resolved) => u(resolved, deserialize(ctx, req)) match {
                            case Some(updated) => Ok ~> (ctx, updated)
                            case _ => ErrPutCannotUpdate
                        }
                    }
                }
            }

            // GET with id requests single resource
            //
            case req @ GET(Path(_)) => {
                _getter lift ctx match {
                    case None => reject(ctx, ErrGetUnauthorized)
                    case Some(g) => {
                        g(rid) match {
                            case None => ErrGetCannotResolveId
                            case Some(found) => Ok ~> (ctx, found)
                        }
                    }
                }
            }

            // DELETE resource by id
            //
            case req @ DELETE(Path(_)) => {
                _deleter lift ctx match {
                    case None => reject(ctx, ErrDeleteUnauthorized)
                    case Some(d) => resolve(rid) match {
                        case None => ErrDeleteCannotResolveId
                        case Some(resolved) =>
                            if (d(resolved)) Ok else ErrDeleteCannotDelete
                    }
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
                _rawCreator lift ctx match {
                    case None => {
                        _creator lift ctx match {
                            case None => reject(ctx, ErrPostUnauthorized)
                            case Some(c) => c(deserialize(ctx, req)) match {
                                case None => ErrPostCannotCreate
                                case Some(created) => Created ~> (ctx, created)
                            }
                        }
                    }
                    case Some(raw) => raw(req) match {
                        case None => ErrPostCannotDeser
                        case Some(created) => Created ~> (ctx, created)
                    }
                }
            }

            // GET w/o id is a query
            //
            case req @ GET(_) => {

                /*
                 * query 'count' flag extractor
                 */
                object Count extends b.uf.params.Flag("count")

                req match {

                    // count flag is true - we only want the
                    // total # of resources that the query represents
                    //
                    case Params(Count(flag)) if (flag) => _counter lift (ctx) match {
                        case None => reject(ctx, ErrCountUnauthorized)
                        case Some(c) => Ok ~> (ctx -> c())
                    }

                    case _ => _querier lift ctx match {
                        case None => reject(ctx, ErrGetUnauthorized)
                        case Some(q) => {

                            /*
                             * 'page' and 'size' param extractors
                             */
                            object Page extends Params.Extract("page", Params.first ~> Params.int)
                            object Size extends Params.Extract("size", Params.first ~> Params.int)

                            req match {
                                // page and size present -
                                // paginate query results accordingly
                                //
                                case Params(Page(p) & Size(s)) => {
                                    val sz = MaxPageSize match {
                                        case Some(max) => if (max > s) s else max
                                        case None => s
                                    }
                                    Ok ~> (ctx -> q(QueryParams(Some(p), Some(sz))))
                                }
                                // only size present -
                                // limit query results accordingly
                                //
                                case Params(Size(s)) => {
                                    val sz = MaxPageSize match {
                                        case Some(max) => if (max > s) s else max
                                        case None => s
                                    }
                                    Ok ~> (ctx -> q(QueryParams(None, Some(sz))))
                                }
                                // no pagination params - only limit
                                // query results by MaxPageSize value
                                //
                                case _ => {
                                    MaxPageSize match {
                                        case Some(max) => Ok ~> (ctx -> q(QueryParams(None, Some(max))))
                                        case None => Ok ~> (ctx -> q(QueryParams(None, None)))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // fall through
            //
            case _ => fail(ctx)
        }
    }

    private def fail(ctx: Context[T]): ErrorResponse = ctx.req match {

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

object ErrPostUnauthorized extends ErrorResponse(Unauthorized, 1000, "unauthorized")
object ErrPostCannotCreate extends ErrorResponse(BadRequest, 1001, "unable to create resource")
object ErrPostCannotDeser extends ErrorResponse(BadRequest, 1002, "unable to deserialize resource from content")
object ErrPostNotJson extends ErrorResponse(UnsupportedMediaType, 1003, "content-type must be application/json")

object ErrPutUnauthorized extends ErrorResponse(Unauthorized, 2000, "unauthorized")
object ErrPutCannotResolveId extends ErrorResponse(NotFound, 2001, "unknown id")
object ErrPutCannotUpdate extends ErrorResponse(BadRequest, 2002, "unable to update resource")
object ErrPutMissingId extends ErrorResponse(NotFound, 2004, "resource id required in path")
object ErrPutNotJson extends ErrorResponse(UnsupportedMediaType, 2005, "content-type must be application/json")

object ErrGetUnauthorized extends ErrorResponse(Unauthorized, 3000, "unauthorized")
object ErrCountUnauthorized extends ErrorResponse(Unauthorized, 3001, "unauthorized")
object ErrGetCannotResolveId extends ErrorResponse(NotFound, 3002, "unknown id")
object ErrGetMissingId extends ErrorResponse(NotFound, 3003, "resource id required in path")

object ErrDeleteUnauthorized extends ErrorResponse(Unauthorized, 4000, "unauthorized")
object ErrDeleteCannotResolveId extends ErrorResponse(NotFound, 4001, "unknown id")
object ErrDeleteCannotDelete extends ErrorResponse(BadRequest, 4002, "unable to delete resource")
object ErrDeleteMissingId extends ErrorResponse(NotFound, 4003, "resource id required in path")

class ErrBadMethod(val err: String) extends ErrorResponse(MethodNotAllowed, 9999, err)
object ErrBadMethod { def apply(method: String) = new ErrBadMethod("%s not supported" format method) }

/**
 * A helper that will JSON serialize BigDecimal
 */
import net.liftweb.json._
object BigDecimalSerializer extends Serializer[BigDecimal] {
    private val Class = classOf[BigDecimal]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), BigDecimal] = {
        case (TypeInfo(Class, _), json) => json match {
            case JInt(iv) => BigDecimal(iv)
            case JDouble(dv) => BigDecimal(dv)
            case value => throw new MappingException("Can't convert " + value + " to " + Class)
        }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
        case d: BigDecimal => JDouble(d.doubleValue)
    }
}
