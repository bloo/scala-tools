name := "slick"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val slick = "1.0.1"
  val javaxval = "1.0.0.GA"
  Seq(
  "com.typesafe.slick" %% "slick" % slick,
  "javax.validation" % "validation-api" % javaxval
  )
}

