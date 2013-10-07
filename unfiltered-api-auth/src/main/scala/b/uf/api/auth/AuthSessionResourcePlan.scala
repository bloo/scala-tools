package b.uf.api.auth

import org.joda.time.DateTime
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.pretty
import net.liftweb.json.render
import unfiltered.request.BasicAuth
import unfiltered.request.HttpRequest
import b.uf.api.ResourcePlan


class AuthSessionResourcePlan[T,S] extends ResourcePlan[T,S](1, "auth", "sessions")
	with BasicResourceAuthComponent[T,S] {
    this: TokenComponent[T] with SessionComponent[T,S] =>
    
	// we can get, create, or delete a session only if we've been authenticated
	//
    def authorizeSave[A](auth: Option[T], req: HttpRequest[A]): Boolean = permIfAuth(auth)
    def authorizeDelete[A](auth: Option[T], req: HttpRequest[A], id: String): Boolean = permIfAuth(auth)
    def authorizeGet[A](auth: Option[T], req: HttpRequest[A], id: String): Boolean = permIfAuth(auth)
    def authorizeGetAll[A](auth: Option[T], req: HttpRequest[A]): Boolean = false
    
    // no updating, ever
	//
    def authorizeUpdate[A](auth: Option[T], req: HttpRequest[A], id: String): Boolean = false

    // this is called when we're prepping for 'save' - if we're authenticated, create the session
    // here and when we get to 'save', we're just going to return this created session resource
    //
    def deserialize[A](auth: Option[T], req: HttpRequest[A], data: String): Option[S] = {
    	case class LoginRequest(val remember: Boolean)
        val lr = fromJson[LoginRequest](data)
        auth match {
	        case Some(acct) => Some(sessionService.create(acct, lr.remember))
	        case _ => None
        }
    }

    def query[A](req: HttpRequest[A], page: Option[Int], size: Option[Int]): List[S] = Nil
    def count[A](req: HttpRequest[A]): Int = 0
    
    // find our user's session, ignore 'id', as it's "local"
    //
    def find(id: String): Option[S] = if ("local" == id) sessionService.local else sessionService get id
    
    // session already 'saved' via deserialization(req)
    //
    def save(resource: S): Option[S] = Some(resource)
    
    // there is no updating of session anyway - this is never called
    //
    def update(original: S, resource: S): Option[S] = None

    // delete will unauthenticate, that is remove, the session
    //
    def delete(resource: S): Boolean = sessionService.remove(resource)

}

