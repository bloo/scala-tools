package b.uf.api.storage

import b.storage._
import b.uf.api._

abstract class StorageResource[T](path: String, maxPageSize: Option[Int] = None)(implicit mf: Manifest[T])
    extends Resource[T, ObjectMeta[T]](path, maxPageSize) 
    with b.log.Logger {

    this: ResourceAuthComponent[T] with StorageComponent[T] =>

    def _auth(ctx: Context[T]) = ctx.hasAuth && true

    import unfiltered.request.Params

    import unfiltered.request.Params
    object Name extends Params.Extract("name", Params.first ~> Params.nonempty)

    def resolve = {
        case (_,id) => storageService.find(id)
    }
    
    override implicit def deserializeCreateReq(cr: CreateRequest)
    	(implicit mf: Manifest[ObjectMeta[T]]): ObjectMeta[T] = {
        // TODO finish and test
        super.deserializeCreateReq(cr)
    }
    
    create {
        case (ctx) if _auth(ctx) => { _ =>
            ctx req match {
                case Params(Name(name)) => storageService.request(ctx.auth.get, name)
                case _ => None
            }
        }
    }

    get {
        case (ctx) if _auth(ctx) => Some(_)
    }

    delete {
        case (ctx) if _auth(ctx) => { om => storageService.delete(ctx.auth.get, om.id) }
    }
}