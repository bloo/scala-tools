name := "unfiltered-api-core"

libraryDependencies <<= (organization, version, scalaVersion) { (o, v, sv) =>
  val lift = "2.5.1"
  Seq(
  "net.liftweb" %% "lift-json" % lift,
  o %% "unfiltered-core" % v
  )
}

