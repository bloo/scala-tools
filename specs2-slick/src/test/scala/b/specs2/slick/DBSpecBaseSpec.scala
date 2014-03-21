package b.specs2.slick

import b.slick._
import org.specs2._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PostgresDBSpecBaseSpec extends {
	override val name: DB.Name = "specs2-slick-base-test-postgres"	
} with DBSpecBase {

	import simple._
	case class Member(id: Long, email: String, name: Option[String])
	class Members(tag: Tag) extends DBTable[Member](tag, randomTableName) {
		def id = column[Long]("id", O.NotNull, O.PrimaryKey)
		def email = column[String]("email", O.NotNull)
		def name = column[String]("name", O.Nullable)
		def * = (id, email, name?) <> (Member.tupled, Member.unapply _)
	}
	
	"DBSpecBase for PostgreSQL" should {
		"access the backend correctly" in tx { implicit s: Session =>
			val members = TableQuery[Members]
			members.ddl.create
			members += Member(1L, "foo@test.com", None)
			members += Member(2L, "bar@baz.net", Some("Bar Smith"))
			members.list.foreach { println(_) }
			members.list.size mustEqual 2
		}
	}
}

@RunWith(classOf[JUnitRunner])
class MysqlDBSpecBaseSpec extends {
	override val name: DB.Name = "specs2-slick-base-test-mysql"	
} with DBSpecBase {

	import simple._
	case class Member(id: Long, email: String, name: Option[String])
	class Members(tag: Tag) extends DBTable[Member](tag, randomTableName) {
		def id = column[Long]("id", O.NotNull, O.PrimaryKey)
		def email = column[String]("email", O.NotNull)
		def name = column[String]("name", O.Nullable)
		def * = (id, email, name?) <> (Member.tupled, Member.unapply _)
	}
	
	"DBSpecBase for MySQL" should {
		"access the backend correctly" in tx { implicit s: Session =>
			val members = TableQuery[Members]
			members.ddl.create
			members += Member(1L, "foo@test.com", None)
			members += Member(2L, "bar@baz.net", Some("Bar Smith"))
			members.list.foreach { println(_) }
			members.list.size mustEqual 2
		}
	}
}

@RunWith(classOf[JUnitRunner])
class H2DBSpecBaseSpec extends {
	override val name: DB.Name = "specs2-slick-base-test-h2"	
} with DBSpecBase {
	
	import simple._
	case class Member(id: Long, email: String, name: Option[String])
	class Members(tag: Tag) extends DBTable[Member](tag, randomTableName) {
		def id = column[Long]("id", O.NotNull, O.PrimaryKey)
		def email = column[String]("email", O.NotNull)
		def name = column[String]("name", O.Nullable)
		def * = (id, email, name?) <> (Member.tupled, Member.unapply _)
	}
		
	"DBSpecBase for H2" should {
		
		"access an h2 db file" in tx { implicit s: Session =>
			val members = TableQuery[Members]
			members.ddl.create
			members += Member(1L, "foo@test.com", None)
			members += Member(2L, "bar@baz.net", Some("Bar Smith"))
			members.list.foreach { println(_) }
			members.list.size mustEqual 2
		}
	}
	
}
