package b.specs2.uf.api.auth

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import b.uf.api.auth._

case class Token(user: String, pass: String)
case class Session(id: String, user: String)

@RunWith(classOf[JUnitRunner])
class AuthSessionResourcePlanSpec
    extends b.specs2.uf.api.ResourcePlanSpecBase[Token, Session, AuthSessionResourcePlan[Token, Session]] {

    val requestingToken = Token("user", "pass")
    // implicit 'requester' for all http calls
    implicit val requester = Some(requestingToken.user -> requestingToken.pass)

    def resourcePlan = new b.uf.api.auth.AuthSessionResourcePlan[Token, Session] with TestAuthComponent[Token, Session] {
        def credentialsToToken = (u, p) => Token(u, p)
        def tokenToSession = (id, token) => Session(id, token.user)
        def sessionToId = _.id
    }

    val LoginJsonContent = """{"remember":false}"""

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
            val (status, _) = xget(sess.id)
            status must be_==(404)
        }

        "not find a made-up session" in {
            val (status, _) = xget("abcd1234")
            status must be_==(404)
        }
    }
}

