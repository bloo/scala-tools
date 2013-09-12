name := "specs2-core"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val junit = "4.8.2"
  val specs2 = "2.1.1"
  val mockito = "1.9.5"
  val unfiltered = "0.7.0-beta1"
  Seq(
  //
  "junit" % "junit" % junit,
  "org.specs2" %% "specs2" % specs2,
  "org.mockito" % "mockito-core" % mockito,
  "net.databinder" %% "unfiltered-specs2" % unfiltered
  )
}

