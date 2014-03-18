name := "unfiltered-core"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val unfiltered = "0.7.1"
  Seq(
  "net.databinder" %% "unfiltered" % unfiltered,
  "net.databinder" %% "unfiltered-filter" % unfiltered,
  "net.databinder" %% "unfiltered-jetty" % unfiltered
  )
}

