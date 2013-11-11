package b.uf.api

object ObjectStatus extends Enumeration {
    type ObjectStatus = Value
    val ready, uploading, completed, error = Value
}

import ObjectStatus._
case class ObjectMeta(
        val id: String,
        val name: String,
        val url: String,
        val size: Int,
        val status: ObjectStatus, val error: Option[String])

trait ObjectComponent[T] {

    val objectService: ObjectService[T]

    trait ObjectService[T] {
        def authorized(owner: T, objectType: String): Boolean
        def create(owner: T, objectType: String, name: String): ObjectMeta
        def status(id: String): Option[ObjectMeta]
        def destroy(id: String): Boolean
    }
}

abstract class ObjectResourcePlan[T](
    Version: Int,
    Group: String,
    ResourcePath: String,
    MaxPageSize: Option[Int] = None)
    extends ResourcePlan[T, ObjectMeta](Version, Group, ResourcePath, MaxPageSize) 
    with b.common.Logger {
    this: ResourceAuthComponent[T] with ObjectComponent[T] =>

    def _auth(ctx: Context[T]) =
        ctx.hasAuth && objectService.authorized(ctx.auth.get, Group)

    import unfiltered.request.Params

    import unfiltered.request.Params
    object Name extends Params.Extract("name", Params.first ~> Params.nonempty)

    def resolve(id: String) = objectService.status(id)
    
    rcreate {
        case (ctx) if _auth(ctx) => { _ =>
            ctx req match {
                case Params(Name(name)) =>
                	Some(objectService.create(ctx.auth.get, Group, name))
                case _ => None
            }
        }
    }

    get {
        case (ctx) if _auth(ctx) => { resolve _ }
    }

    delete {
        case (ctx) if _auth(ctx) => { om =>
            objectService.destroy(om.id)
        }
    }
}