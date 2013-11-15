package b.uf.api.auth

import b.uf.api.ResourcePlan
import org.joda.time.DateTime
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.pretty
import net.liftweb.json.render
import unfiltered.request.{ BasicAuth, Params }
import unfiltered.request.HttpRequest

class AuthSessionResourcePlan[T, S](v: Double, path: String) extends ResourcePlan[T, S](v, path)
    with BasicResourceAuthComponent[T, S] {
    this: TokenComponent[T] with SessionComponent[T, S] =>

    // find our user's session, ignore 'id', as it's "local"
    //
    def resolve(id: String): Option[S] =
        if ("local" == id) sessionService.local else sessionService get id

    object Remember extends b.uf.params.Flag("remember")
    
    rcreate {
        case (ctx) => { _ =>
            ctx auth match {
                // if request was auth'ed, we'll create a
                // session from the auth'd token
                //
                case Some(token) => {
                    val rem = ctx req match {
                    	case Params(Remember(flag)) => flag
                    	case _ => false
                    }
                    Some(sessionService.create(token, rem))
                }
                case _ => None
            }
        }
    }

    get {
        // allow get, simply call resolve
        case (ctx) if ctx hasAuth => resolve _
    }

    delete {
        // delete will unauthenticate, that is remove, the session
        case (ctx) if ctx hasAuth => sessionService.remove(_)
    }
}

