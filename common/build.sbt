name := "common"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val grizzly = "1.0.1"
  val slf4j = "1.6.1"
  val log4j = "1.2.16"
  val joda = "2.3"
  val jodaconvert = "1.4"
  val commons_io = "2.4"
  Seq(
  "org.clapper" %% "grizzled-slf4j" % grizzly,
  "log4j" % "log4j" % log4j,
  "org.slf4j" % "slf4j-api" % slf4j,
  "org.slf4j" % "jcl-over-slf4j" % slf4j,
  "org.slf4j" % "slf4j-log4j12" % slf4j,
  "joda-time" % "joda-time" % joda,
  "org.joda" % "joda-convert" % jodaconvert,
  "commons-io" % "commons-io" % commons_io
  )
}

