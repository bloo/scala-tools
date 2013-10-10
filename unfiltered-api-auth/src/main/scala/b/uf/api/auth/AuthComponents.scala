package b.uf.api.auth

import unfiltered.request.BasicAuth

import b.uf.api.auth._
import org.specs2.mock._

trait TestAuthComponent[T,R] extends BasicResourceAuthComponent[T,R]
		with TokenComponent[T] with SessionComponent[T,R] with Mockito {

    // need to override
    def credentialsToToken: (String,String) => T
    def tokenToSession: (String,T) => R
    def sessionToId: R => String

    override def sessionService = ss
    override def tokenService = ts
    
    val ts = mock[TokenService[T]]
    
    var localT: Option[T] = None
    var localS: Option[R] = None
    val sessMap = scala.collection.mutable.Map[String, R]()
    val ss = new SessionService[T,R] {
    	def localToken = localT
    	def local = localS
    	def create(t: T, remember: Boolean) = {
            localT = Some(t)
            val id = java.util.UUID.randomUUID.toString.replaceAll("-", "")
            val sess = tokenToSession(id,t)
            localS = Some(sess)
            sessMap(id) = sess
            sess        	    
    	}
        def get(id: String) = sessMap get id
        def remove(sess: R) = {
            val removed = sessMap remove sessionToId(sess)
            val wasLocal = localS match {
                case Some(s) => { localS = None; true }
                case _ => false
            }
            wasLocal || (removed match {
                case Some(_) => true
                case _ => false
            })
        }
    }

    // mock up tokenService calls
    //
    org.mockito.Mockito.doReturn(None) when tokenService lookup (anyString, anyString)
    def addTestUser(user: (String,String)) = {
        val u = user._1
        val p = user._2
        org.mockito.Mockito.doReturn(Some(credentialsToToken(u,p))) when tokenService lookup (u, p)
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
trait BasicResourceAuthComponent[T,S] extends b.uf.api.ResourceAuthComponent[T] with b.common.Logger {
	this: TokenComponent[T] with SessionComponent[T,S] =>

	def authService: AuthService[T] = new BasicAuthService

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
