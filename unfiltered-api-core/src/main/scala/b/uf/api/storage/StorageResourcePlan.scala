package b.uf.api.storage

import b.storage._
import b.uf.api._

abstract class StorageResourcePlan[T](
    Version: Int,
    Group: String,
    ResourcePath: String,
    MaxPageSize: Option[Int] = None)
    extends ResourcePlan[T, ObjectMeta[T]](Version, Group, ResourcePath, MaxPageSize) 
    with b.log.Logger {
    this: ResourceAuthComponent[T] with StorageComponent[T] =>

    def _auth(ctx: Context[T]) = ctx.hasAuth && true

    import unfiltered.request.Params

    import unfiltered.request.Params
    object Name extends Params.Extract("name", Params.first ~> Params.nonempty)

    def resolve(id: String) = storageService.find(id)
    
    rcreate {
        case (ctx) if _auth(ctx) => { _ =>
            ctx req match {
                case Params(Name(name)) => storageService.request(ctx.auth.get, name)
                case _ => None
            }
        }
    }

    get {
        case (ctx) if _auth(ctx) => { resolve _ }
    }

    delete {
        case (ctx) if _auth(ctx) => { om => storageService.delete(ctx.auth.get, om.id) }
    }
}