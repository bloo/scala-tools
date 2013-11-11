package b.specs2.uf.api.auth

import unfiltered.request.BasicAuth

import b.uf.api.auth._
import org.specs2.mock._

trait TestAuthComponent[T,R] extends BasicResourceAuthComponent[T,R]
		with TokenComponent[T] with SessionComponent[T,R] with Mockito {

    // need to override
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
    def mockTokenLookup(u: String, p: String, token: T) = {
        org.mockito.Mockito.doReturn(Some(token)) when tokenService lookup (u, p)
    }
}

