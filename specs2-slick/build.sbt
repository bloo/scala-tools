name := "specs2-slick"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val postgres = "9.3-1101-jdbc41"
  val mysql = "5.1.29"
  val h2 = "1.3.175"
  Seq(
    "org.postgresql" % "postgresql" % postgres % "test",
    "mysql" % "mysql-connector-java" % mysql % "test",
    "com.h2database" % "h2" % h2 % "test"
  )
}
