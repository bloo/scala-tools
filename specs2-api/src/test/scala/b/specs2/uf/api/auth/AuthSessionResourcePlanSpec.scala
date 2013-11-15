package b.specs2.uf.api.auth

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import b.uf.api.auth._

case class Token(user: String, pass: String)
case class Session(id: String, user: String)

@RunWith(classOf[JUnitRunner])
class AuthSessionResourcePlanSpec
    extends b.specs2.uf.api.ResourcePlanSpecBase[Token, Session, AuthSessionResourcePlan[Token, Session]] {

    val MockUser = Token("user", "pass")
    // implicit 'requester' for all http calls
    implicit val testRequester = Some(MockUser.user -> MockUser.pass)
    lazy val resourcePlan = new b.uf.api.auth.AuthSessionResourcePlan[Token, Session](1.0, "auth/sessions")
        with TestAuthComponent[Token, Session] {

        def tokenToSession = (id, token) => Session(id, token.user)
        def sessionToId = _.id

        // requesting user is valid user during lookup
        mockTokenLookup(MockUser.user, MockUser.pass, MockUser)
    }

    val LoginJsonContent = ("remember" -> "true")
    
    "AuthSessionResourcePlan" should {

        "authorize valid login request" in {
            val sess = post("", LoginJsonContent)
            sess.user must be_==(MockUser.user)
        }

        "authorize and retrieve authorized session" in {
            val sess = post("", LoginJsonContent)
            val sess2 = get(sess.id)
            sess.id must be_==(sess2.id)
            sess.user must be_==(sess2.user)
        }

        "authorize and unauthorize session" in {
            val sess = post("", LoginJsonContent)
            val sess2 = get(sess.id)
            sess.id must be_==(sess2.id)
            delete(sess.id)
            val (status, _) = xget(sess.id)
            status must be_==(404)
        }

        "not find a made-up session" in {
            val (status, _) = xget("abcd1234")
            status must be_==(404)
        }
    }
}

