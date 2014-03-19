name := "specs2-slick"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val postgres = "9.1-901-1.jdbc4"
  val mysql = "5.1.29"
  val h2 = "1.3.175"
  Seq(
    "postgresql" % "postgresql" % postgres % "test",
    "mysql" % "mysql-connector-java" % mysql % "test",
    "com.h2database" % "h2" % h2 % "test"
  )
}

sourceManaged in Compile <<= (sourceDirectory in Compile)(_ / "resources")

sourceManaged in Test <<= (sourceDirectory in Test)(_ / "resources")
