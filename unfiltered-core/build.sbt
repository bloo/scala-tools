name := "unfiltered-core"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val unfiltered = "0.7.0-beta1" // "0.6.8"
  val scalate = "1.6.1"
  val scalamd = "1.6"
  Seq(
  "net.databinder" %% "unfiltered" % unfiltered,
  "net.databinder" %% "unfiltered-filter" % unfiltered,
  "net.databinder" %% "unfiltered-jetty" % unfiltered,
  "org.fusesource.scalate" %% "scalate-core" % scalate exclude("org.scala-lang", "scala-compiler"),
  "org.fusesource.scalamd" %% "scalamd" % scalamd
  )
}

