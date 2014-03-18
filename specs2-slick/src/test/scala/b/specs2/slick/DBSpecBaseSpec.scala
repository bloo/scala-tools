package b.specs2.slick

import b.slick._
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DBSpecBaseSpec extends {
	override val name: DB.Name = "specs2-slick-base-test"	
} with PostgresSpecBase {
	
	import simple._
	case class Member(id: Long, email: String, name: Option[String])
	class Users(tag: Tag) extends DBTable[Member](tag, "member") {
		def id = column[Long]("id", O.NotNull, O.PrimaryKey)
		def email = column[String]("email", O.NotNull)
		def name = column[String]("name", O.Nullable)
		def * = (id, email, name?) <> (Member.tupled, Member.unapply _)
	}

	"DBSpecBase" should {
		"access the backend correctly" in tx { implicit s: Session =>
			val users = TableQuery[Users]
			users.ddl.create				
			users += Member(1L, "foo@test.com", None)
			users += Member(2L, "bar@baz.net", Some("Bar Smith"))
			users.list.foreach { u => println(u) }
			users.list.size mustEqual 2
		}
	}	
}
