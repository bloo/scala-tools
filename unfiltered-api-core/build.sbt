name := "unfiltered-api-core"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val lift = "2.5.1"
  Seq(
  "net.liftweb" %% "lift-json" % lift
  )
}

