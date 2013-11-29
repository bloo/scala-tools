
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
import b.uf.errors._

/**
 * A helper that will JSON serialize BigDecimal
 */
import net.liftweb.json._
import unfiltered.filter.request.ContextPath

case class Context[T](req: HttpRequest[_], auth: Option[T], pathIds: Map[String, String]) {
    def hasAuth = !auth.isEmpty
}
case class PageParams(page: Option[Int] = None, size: Option[Int])
case class CountResult(val count: Int)
case class ResourceErrorJson(val code: Int, val messages: Seq[String])
case class QueryResultGroup[R](val group: Seq[R])

trait ResourceSerializer[T,R] {
    def ser: PartialFunction[Context[T], Either[R=>String,ResourceErrorJson]]
}

trait ResourceCountSerializer[T] {
    def ser: PartialFunction[Context[T], Either[CountResult=>String,ResourceErrorJson]]
}

trait ResourceQuerySerializer[T,R] {
    def ser: PartialFunction[Context[T], Either[Seq[R]=>String,ResourceErrorJson]]
    def serGroup: PartialFunction[Context[T], Either[QueryResultGroup[R]=>String,ResourceErrorJson]]
}

trait ResourceDeserializer[T,R] {
    def deser: PartialFunction[Context[T], Either[(String,Manifest[R])=>R,ResourceErrorJson]]
}

object Resource {

    private val foo = ""
        
    def apply[T](group: String, version: Double, cb: unfiltered.jetty.ContextBuilder)(resources: Resource[T, _]*) = {
        val list = resources map { res => (res -> res.plan(group, version)) } sortBy {
            case (res, plan) => res.FullPath
        }
        list.reverse.foreach { case (res, plan) => cb.filter(plan) }
    }

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

    def toXml(rootName: String, obj: Any): String = {
        implicit val formats = DefaultFormats + BigDecimalSerializer // brings in default date formats etc.
        val doc = decompose(obj)
        Xml toXml (doc) toString
    }

    // helper json deserializer
    //
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = {
        import net.liftweb.json._
        import net.liftweb.json.Extraction._
        implicit val formats = DefaultFormats + BigDecimalSerializer // Brings in default date formats etc.
        extract[O](parse(json))
    }

    private trait LowPriorityResourceImplicits[T,R] {

        implicit def serResourceToJsonOrXml: ResourceSerializer[T,R] = 
            new ResourceSerializer[T,R] with JsonOrXml[R] {
	    	def ser = _.req match {
	    	    case Accepts.Json(_) => Left(_toJson)
	    	    case Accepts.Xml(_) => Left(_toXml)
	    	    case _ => Right(ResourceErrorJson(0,Seq()))
	    	}
	    }
		
		implicit def serResourcesToJsonOrXml: ResourceQuerySerializer[T,R] =
		    new ResourceQuerySerializer[T,R] with JsonOrXml[R] {
	    	def ser = _.req match {
	    	    case Accepts.Json(_) => Left(_toJson)
	    	    case Accepts.Xml(_) => Left(_toXml)
	    	    case _ => Right(ResourceErrorJson(0,Seq()))
	    	}
	    	def serGroup = _.req match {
	    	    case Accepts.Json(_) => Left(_toJson)
	    	    case Accepts.Xml(_) => Left(_toXml)
	    	    case _ => Right(ResourceErrorJson(0,Seq()))
	    	}
	    }
		
	    implicit def deserFromJson: ResourceDeserializer[T,R] =
	        new ResourceDeserializer[T,R] with JsonOrXml[R] {
	    	def deser = _.req match {
	    	    case RequestContentType("application/json") => Left(_fromJson(_)(_))
	    	    case _ => Right(ResourceErrorJson(0,Seq()))
	    	}	    	
	    }
		
	    trait JsonOrXml[R] {
	        import net.liftweb.json._
	        import net.liftweb.json.Extraction._
	        // brings in default date formats etc.
	        implicit val formats = DefaultFormats + BigDecimalSerializer
	        protected def _toJson(obj: Any): String = {
		        val doc = render(decompose(obj))
		        //if (!prettyJson) pretty(doc) else compact(doc)  
		        compact(doc)
	        }
	        protected def _toXml(obj: Any): String = {
		        val doc = decompose(obj)
		        Xml toXml (doc) toString
	        }
	        protected def _fromJson(data: String)(implicit mf: Manifest[R]): R = {
	            extract[R](parse(data))
	        }
	    }	    
	}    
}

abstract class Resource[T, R](
    val resourcePath: String,
    val maxPageSize: Option[Int] = None)
    extends b.log.Logger {//with Resource.LowPriorityResourceImplicits[R] {

    this: ResourceAuthComponent[T] =>

    private var _group: Option[String] = None
    private var _version: Option[Double] = None

    // helper
    //
    implicit def _isNum(id: String) = new { def isNum = id forall Character.isDigit }
    implicit def _isAlpha(id: String) = new { def isAlpha = id forall Character.isLetter }
    implicit def _isAlphaNum(id: String) = new { def isAlphaNum = id forall Character.isLetterOrDigit }

    def plan(version: Double): Plan = plan(None, version)
    def plan(group: String, version: Double): Plan = plan(Some(group), version)
    def plan(group: Option[String], version: Double): Plan = {
        _group = group
        _version = Some(version)
        val p = new Plan {
            def intent = {
                unfiltered.kit.Routes.specify(
                    FullPath -> dointent _,
                    FullPath + ("/:%s" format ResourceIdParam) -> dointent _)
            }
        }
        _postConfigHooks foreach { _() }
        p
    }

    type Hook = Function0[_ <: Any]
    private val _postConfigHooks = collection.mutable.ArrayBuffer[Hook]()
    protected def postConfig(hook: Hook) = _postConfigHooks += hook

    def version = _version.get // always defined after config
    def group = _group // can still be Option after config

    lazy val FullPath = "/%s%s/%s" format (_group match {
        case Some(g) => g + "/"
        case None => ""
    }, "v" + version, resourcePath)
    val ResourceIdParam = "resource_id"

    def toJson(obj: Any): String = Resource toJson obj
    def toXml(obj: Any): String = Resource toXml (resourcePath, obj)
    def fromJson[O](json: String)(implicit mf: Manifest[O]): O = Resource fromJson json

    private def reject(ctx: Context[T], resp: ErrorResponse): ResponseFunction[Any] = ctx.auth match {
        //case Some(_) => Forbidden ~> resp // authenticated but unauthorized
        //case _ => WWWAuthenticate(Realm) ~> resp // might need authentication
        case _ => Forbidden ~> resp // don't use wwwauthenticate for httpbasic, to avoid popup
    }

    // subclass needs to implement resource resolver
    //
    type Resolver = PartialFunction[(Context[T], String), Option[R]]
    def resolve: Resolver

    type Creator = PartialFunction[Context[T], R => Option[R]]
    protected var _creator: Creator = Map.empty
    def create(c: Creator) = _creator = c

    type RawCreator = PartialFunction[Context[T], String => Option[R]]
    protected var _rawCreator: RawCreator = Map.empty
    def rcreate(c: RawCreator) = _rawCreator = c
//    def rcreate(c: RawCreator)(implicit ds: ResourceDeserializer[T], s: ResourceSerializer[T]) = _rawCreator = c

    type Getter = PartialFunction[Context[T], R => Option[R]]
    protected var _getter: Getter = Map.empty
    def get(g: Getter) = _getter = g
//    protected var _getterDeser: ResourceDeserializer[R] = null
//    protected var _getterSer: ResourceSerializer[R] = null
//    def get(g: Getter)(implicit ds: ResourceDeserializer[R], s: ResourceSerializer[R]) = {
//        _getter = g
//        _getterDeser = ds
//        _getterSer = s
//    }

    type Updater = PartialFunction[Context[T], (R, R) => Option[R]]
    protected var _updater: Updater = Map.empty
    def update(u: Updater) = _updater = u
//    protected var _updaterDeser: ResourceDeserializer[R] = null
//    protected var _updaterSer: ResourceSerializer[R] = null
//    protected var _updater: Updater = Map.empty
//    def update(u: Updater)(implicit ds: ResourceDeserializer[R], s: ResourceSerializer[R]) = {
//        _updater = u
//        _updaterDeser = ds
//        _updaterSer = s
//    }

    type Deleter = PartialFunction[Context[T], R => Boolean]
    protected var _deleter: Deleter = Map.empty
    def delete(d: Deleter) = _deleter = d
//    def delete(d: Deleter)(implicit ds: ResourceDeserializer[R]) = { _deleter = d; _deleterDeser = ds }
//    protected var _deleterDeser: ResourceDeserializer[R] = null

    type Counter = PartialFunction[Context[T], () => Int]
    protected var _counter: Counter = Map.empty
    def count(c: Counter) = _counter = c
//    def count(c: Counter)(implicit s: ResourceCountSerializer) = { _counter = c; _counterSer = s }
//    protected var _counterSer: ResourceCountSerializer = null

    type Querier = PartialFunction[Context[T], PageParams => Seq[R]]
    protected var _querier: Querier = Map.empty
    def query(q: Querier) = _querier = q
//    def query(q: Querier)(implicit s: ResourceQuerySerializer[R]) = { _querier = q; _querierSer = s }
//    protected var _querierSer: ResourceQuerySerializer[R] = null

    // resource converters; by default they will use the from|toJson
    // helper methods, but they can be overridden for custom handling.
    //
    def deserialize(ctx: Context[T], data: String): R = fromJson(data)

    def serializeGet(ctx: Context[T], resource: R): String = ctx.req match {
        case Accepts.Json(_) => toJson(resource)
        case Accepts.Xml(_) => toXml(resource)
    }

    def serializeCount(ctx: Context[T], cnt: CountResult): String = ctx.req match {
        case Accepts.Json(_) => toJson(cnt)
        case Accepts.Xml(_) => toXml(cnt)
    }

    def serializeQuery(ctx: Context[T], resources: Seq[R]): String = ctx.req match {
        case Accepts.Json(_) => toJson(resources)
        case Accepts.Xml(_) => toXml(resources)
    }

    def serializeQueryGroup(ctx: Context[T], resources: Seq[QueryResultGroup[R]]): String = ctx.req match {
        case Accepts.Json(_) => toJson(resources)
        case Accepts.Xml(_) => toXml(resources)
    }

    // implicit defs that take Context and resource tuples and convert them
    // into JSON responses using the serialization helpers above.
    //
    private implicit def resourceToResponse(cr: (Context[T], R)): ResponseFunction[Any] =
        JsonContent ~> ResponseString(serializeGet(cr._1, cr._2))

    private implicit def resourcesToResponse(cr: (Context[T], Seq[R], Option[Int], Option[Int], Boolean)): ResponseFunction[Any] = {
        val (ctx, results, groups, groupSize, groupTranspose) = cr

        // calculate group cnt and group size depending on which was defined
        //
        val gCntAndSize = (groups, groupSize) match {
            case (Some(gCnt), _) => Some(gCnt -> math.ceil(results.length / gCnt.toDouble).toInt)
            case (None, Some(gSize)) => Some(math.ceil(results.length / gSize.toDouble).toInt -> gSize)
            case _ => None
        }

        val json = gCntAndSize match {
            case Some((gCnt, gSize)) if results.length > 0 => {

                // if transpose is true and gCnt & gSize are defined,
                // we'll reorder the results list so that the .grouped(Seq)
                // method gives us the groupings we want
                //
                val groupMe = if (groupTranspose) {

                    val transposed = new collection.mutable.ArraySeq[R](results.length)
                    // this isn't very elegant.. but at least it's O(n)
                    var start = 0
                    val end = results.length - 1
                    var sourceIdx = 0
                    var destIdx = 0
                    do {
                        transposed update (destIdx, results(sourceIdx))
                        destIdx = destIdx + 1
                        sourceIdx = sourceIdx + gCnt
                        if (sourceIdx > end) { start = start + 1; sourceIdx = start }
                    } while (destIdx <= end)
                    // 'transposed' now contains the same elements as results, but in
                    // an order that a call to .grouped will get us the transposed
                    // grouping that we want
                    transposed
                } else results

                // group query result by requested group size
                //
                val grouped = groupMe.grouped(gSize).toSeq.map(QueryResultGroup(_))
                serializeQueryGroup(ctx, grouped)
            }

            // simply serialize result seq as one ungrouped list
            case None => serializeQuery(ctx, results)
        }
        JsonContent ~> ResponseString(json)
    }

    private implicit def intToResponse(cr: (Context[T], Int)): ResponseFunction[Any] =
        JsonContent ~> ResponseString(serializeCount(cr._1, CountResult(cr._2)))

    private implicit def reqToResource[X](req: HttpRequest[X]): String = {
        val in = req inputStream
        val out = new StringWriter
        IOUtils.copy(in, out)
        IOUtils closeQuietly in
        val resp = out.toString
        IOUtils closeQuietly out
        resp
    }

    private implicit def errorToJson(error: ErrorResponse): ResponseFunction[Any] = {
        val re = ResourceErrorJson(error.code, error.messages)
        val json = toJson(re)
        error.status ~> ResponseString(json)
    }

    private def dointent(req: HttpRequest[javax.servlet.http.HttpServletRequest], params: Map[String, String]) = {
        val ctx = Context[T](req, authService.authenticate(req), params)
        req match {
            case Accepts.Json(_) | Accepts.Xml(_) => authorize(ctx, params.get(ResourceIdParam))(req)
            case _ => NotAcceptable ~> ResponseString("You must accept application/json or application/xml")
        }
    }

    private def authorize(ctx: Context[T], resourceId: Option[String]): Plan.Intent = resourceId match {
        case Some(rid) => {

            // PUT request must contain JSON
            //
            case req @ PUT(Path(_) & RequestContentType("application/json")) => {
                _updater lift ctx match {
                    case None => reject(ctx, ErrPutUnauthorized)
                    case Some(u) => resolve(ctx, rid) match {
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
                    case Some(g) => resolve(ctx, rid) match {
                        case Some(resolved) => g(resolved) match {
                            case None => ErrGetCannotResolveId
                            case Some(found) => Ok ~> (ctx, found)
                        }
                        case None => ErrGetCannotResolveId
                    }
                }
            }

            // DELETE resource by id
            //
            case req @ DELETE(Path(_)) => {
                _deleter lift ctx match {
                    case None => reject(ctx, ErrDeleteUnauthorized)
                    case Some(d) => resolve(ctx, rid) match {
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
                            object GroupCnt extends Params.Extract("groups", Params.first ~> Params.int)
                            object GroupSize extends Params.Extract("groupsize", Params.first ~> Params.int)
                            object GroupTrans extends b.uf.params.Flag("grouptranspose")

                            val groupCnt = req match {
                                case Params(GroupCnt(gc)) => Some(gc)
                                case _ => None
                            }

                            val groupSize = req match {
                                case Params(GroupSize(g)) => Some(g)
                                case _ => None
                            }

                            val groupTranspose = req match {
                                case Params(GroupTrans(flag)) => flag
                                case _ => false
                            }

                            req match {
                                // page and size present -
                                // paginate query results accordingly
                                //
                                case Params(Page(p) & Size(s)) => {
                                    val sz = maxPageSize match {
                                        case Some(max) => if (max > s) s else max
                                        case None => s
                                    }
                                    Ok ~> (ctx, q(PageParams(Some(p), Some(sz))), groupCnt, groupSize, groupTranspose)
                                }
                                // only size present -
                                // limit query results accordingly
                                //
                                case Params(Size(s)) => {
                                    val sz = maxPageSize match {
                                        case Some(max) => if (max > s) s else max
                                        case None => s
                                    }
                                    Ok ~> (ctx, q(PageParams(None, Some(sz))), groupCnt, groupSize, groupTranspose)
                                }
                                // no pagination params - only limit
                                // query results by MaxPageSize value
                                //
                                case _ => {
                                    maxPageSize match {
                                        case Some(max) => Ok ~> (ctx, q(PageParams(None, Some(max))), groupCnt, groupSize, groupTranspose)
                                        case None => Ok ~> (ctx, q(PageParams(None, None)), groupCnt, groupSize, groupTranspose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
