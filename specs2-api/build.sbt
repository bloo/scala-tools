name := "specs2-api"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  Seq(
  "bloo" %% "unfiltered-api-auth" % v
  )
}

