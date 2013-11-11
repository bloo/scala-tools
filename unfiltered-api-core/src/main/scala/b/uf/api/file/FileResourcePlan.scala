package b.uf.api.file

import b.file._
import b.uf.api._

abstract class FileResourcePlan[T](
    Version: Int,
    Group: String,
    ResourcePath: String,
    MaxPageSize: Option[Int] = None)
    extends ResourcePlan[T, FileMeta](Version, Group, ResourcePath, MaxPageSize) 
    with b.common.Logger {
    this: ResourceAuthComponent[T] with FileComponent[T] =>

    def _auth(ctx: Context[T]) =
        ctx.hasAuth && fileService.authorized(ctx.auth.get, Group)

    import unfiltered.request.Params

    import unfiltered.request.Params
    object Name extends Params.Extract("name", Params.first ~> Params.nonempty)

    def resolve(id: String) = fileService.status(id)
    
    rcreate {
        case (ctx) if _auth(ctx) => { _ =>
            ctx req match {
                case Params(Name(name)) =>
                	Some(fileService.create(ctx.auth.get, Group, name))
                case _ => None
            }
        }
    }

    get {
        case (ctx) if _auth(ctx) => { resolve _ }
    }

    delete {
        case (ctx) if _auth(ctx) => { om =>
            fileService.destroy(om.id)
        }
    }
}