package b.uf.api.auth

import unfiltered.request.BasicAuth
import b.uf.api.auth._

trait TokenComponent[T] {
    def tokenService: TokenService[T]
    trait TokenService[T] {
        def lookup(principal: String, secret: String): Option[T]
    }
}

trait SessionComponent[T,S] {
    def sessionService: SessionService[T,S]
    trait SessionService[T,S] {
        def create(token: T, remember: Boolean): S
        def remove(sess: S): Boolean
        def local: Option[S]
        def localToken: Option[T]
        def get(id: String): Option[S]
    }
}

/**
 * cake pattern notes here:
 *  
 * http://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth/
 * 
 * 
 * 
 * 
 *  ResourceAuthComponent implementation based on HTTP BasicAuth
 */

trait BasicResourceAuthComponent[T,S] extends b.uf.api.ResourceAuthComponent[T] with b.log.Logger {
	this: TokenComponent[T] with SessionComponent[T,S] =>

	private lazy val _as = new BasicAuthService
	def authService: AuthService[T] = _as

	class BasicAuthService extends AuthService[T] { 
	    //def realm: String
	    def authenticate[A](req: unfiltered.request.HttpRequest[A]): Option[T] = sessionService.localToken match {
	    	case Some(a) => Some(a)
	        case None => {
	        	req match {
	        	    case BasicAuth(user, pass) => {
	        	    	logger.info("BasicAuth, user=%s, pass=****" format user)
						tokenService lookup(user,pass)
					}
					case _ => None
	        	}
	        }
	    }
	}
}
