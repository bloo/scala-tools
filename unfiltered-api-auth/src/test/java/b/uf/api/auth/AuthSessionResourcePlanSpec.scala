package b.uf.api.auth

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

case class Token(user: String, pass: String)
case class Session(id: String, user: String)

@RunWith(classOf[JUnitRunner])
class TestAuthSessionResourcePlanSpec
	extends AuthSessionResourcePlanSpecBase[Session,Token]{
    
    val requestingToken = Token("user", "pass")
    // implicit 'requester' for all http calls
	val requester = Some(requestingToken.user -> requestingToken.pass)
    def validUsers = List(requester.get)

    def tokenToSession = (id,token) => Session(id, token.user)
    def sessionToId = _.id
    def lookupResponse = (u,p) => Token(u,p)
    
    "AuthSessionResourcePlan" should {

        "authorize valid login request" in {
        	val sess = post(LoginJsonContent)
        	sess.user must be_==(requestingToken.user)
        }

        "authorize and retrieve authorized session" in {
        	val sess = post(LoginJsonContent)
        	val sess2 = get(sess.id)
        	sess.id must be_==(sess2.id)
        	sess.user must be_==(sess2.user)
        }
        
        "authorize and unauthorize session" in {
        	val sess = post(LoginJsonContent)
        	val sess2 = get(sess.id)
        	sess.id must be_==(sess2.id)
        	delete(sess.id)
        	val (status,_) = xget(sess.id)
            status must be_==(404)
        }
        
        "not find a made-up session" in {
            val (status,_) = xget("abcd1234")
            status must be_==(404)
        }
    }
}

abstract class AuthSessionResourcePlanSpecBase[R,T]
	extends b.specs2.uf.api.ResourcePlanSpecBase[R,T,AuthSessionResourcePlan[T,R]] {

    def tokenToSession: (String,T) => R
    def sessionToId: R => String
    def lookupResponse: (String,String) => T
    
    def resourcePlan = new b.uf.api.auth.AuthSessionResourcePlan[T,R] with TestAuthComponent

    import b.uf.api.auth._
    trait TestAuthComponent extends BasicResourceAuthComponent[T,R]
    		with TokenComponent[T] with SessionComponent[T,R] {
        
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
        for ((u,p) <- validUsers)
            org.mockito.Mockito.doReturn(Some(lookupResponse(u,p))) when tokenService lookup (u, p)
    }
    
    val LoginJsonContent = """{"remember":false}"""
 
}
