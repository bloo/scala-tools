name := "slick"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val slick = "2.0.0"
  val javaxval = "1.1.0.Final"
  val dbcp = "2.0"
  Seq(
    "com.typesafe.slick" %% "slick" % slick,
    "javax.validation" % "validation-api" % javaxval,
    "org.apache.commons" % "commons-dbcp2" % dbcp
  )
}
