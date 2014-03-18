package b.specs2.slick

import b.slick._
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DBSpecBaseSpec extends {
	override val name: DB.Name = "specs2-slick-base-test"	
} with PostgresSpecBase("testdb") {
	
	import simple._
	case class Member(id: Long, email: String, name: Option[String])
	class Users(tag: Tag) extends DBTable[Member](tag, "member") {
		def id = column[Long]("id")
		def email = column[String]("email")
		def name = column[String]("name")
		def * = (id, email, name?) <> (Member.tupled, Member.unapply _)
	}

	"DBSpecBase" should {
		"access the backend correctly" in {
			val users = TableQuery[Users]
			readOnly { implicit s =>
				users.list.foreach { u => println(u) }
			}
			success
		}
	}	
}
