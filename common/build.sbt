name := "common"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val grizzly = "1.0.1"
  val joda = "2.2"
  val jodaconvert = "1.4"
  val commons_io = "2.4"
  Seq(
  "org.clapper" %% "grizzled-slf4j" % grizzly,
  "joda-time" % "joda-time" % joda,
  "org.joda" % "joda-convert" % jodaconvert,
  "commons-io" % "commons-io" % commons_io
  )
}

