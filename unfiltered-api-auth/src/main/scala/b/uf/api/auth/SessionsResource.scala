package b.uf.api.auth

import b.uf.api.Resource
import org.joda.time.DateTime
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.pretty
import net.liftweb.json.render
import unfiltered.request.{ BasicAuth, Params }
import unfiltered.request.HttpRequest

class SessionsResource[T, S](path: String = "sessions")
	extends Resource[T, S](path)
    with BasicResourceAuthComponent[T, S] {
    this: TokenComponent[T] with SessionComponent[T, S] =>

    // find our user's session, ignore 'id', as it's "local"
    //
    override def resolve = {
        case (_,id) if ("local" == id) => sessionService.local
        case (_,id) => sessionService get id
    }

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
        case (ctx) if ctx hasAuth => Some(_)
    }

    delete {
        // delete will unauthenticate, that is remove, the session
        case (ctx) if ctx hasAuth => sessionService.remove(_)
    }
}

