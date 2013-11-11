package b.uf.api.auth

import b.uf.api.ResourcePlan
import org.joda.time.DateTime
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.pretty
import net.liftweb.json.render
import unfiltered.request.BasicAuth
import unfiltered.request.HttpRequest

class AuthSessionResourcePlan[T,S] extends ResourcePlan[T,S](1, "auth", "sessions")
	with BasicResourceAuthComponent[T,S] {
    this: TokenComponent[T] with SessionComponent[T,S] =>

	import b.uf.api.Context

    def deserialize(ctx: Context[T], data: String): Option[S] = {
    	case class LoginRequest(val remember: Boolean)
        val lr = fromJson[LoginRequest](data)
        ctx auth match {
	        case Some(acct) => Some(sessionService.create(acct, lr.remember))
	        case _ => None
        }
    }
    
    // find our user's session, ignore 'id', as it's "local"
    //
    def resolve(id: String): Option[S] =
        if ("local" == id) sessionService.local else sessionService get id

    create {
    	// session already 'saved' via deserialization(req)
    	case(_) => { sess => Some(sess) }
    }
    
    get {
        // allow get, simply call resolve
        case (ctx) if ctx hasAuth => { id => resolve(id) }
    }
    
    delete {
    	// delete will unauthenticate, that is remove, the session
        case (ctx) if ctx hasAuth => { sess => sessionService.remove(sess) }
    }
}

