name := "unfiltered-api-auth"

libraryDependencies <<= (organization, version, scalaVersion) { (o, v, sv) =>
  Seq(
  // rely on our own specs2-api test lib to test these resource plans
  //o %% "specs2-api" % v % "test"
  o %% "unfiltered-api-core" % v
  )
}
