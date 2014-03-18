name := "common"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  // logging
  val grizzly = "1.0.1"
  val slf4j = "1.6.1"
  val log4j = "1.2.16"
  // config
  val typesafe_config = "1.2.0"
  // datetime
  val joda = "2.3"
  val joda_convert = "1.4"
  // mail
  val javax_mail = "1.4.7"
  // scalate engine
  val scalate = "1.6.1"
  val scalamd = "1.6"
  // utils
  val commons_io = "2.4"
  // storage
  val jets3t = "0.9.0"
  Seq(
  "org.clapper" %% "grizzled-slf4j" % grizzly,
  "log4j" % "log4j" % log4j,
  "org.slf4j" % "slf4j-api" % slf4j,
  "org.slf4j" % "jcl-over-slf4j" % slf4j,
  "org.slf4j" % "slf4j-log4j12" % slf4j,
  "com.typesafe" % "config" % typesafe_config,
  "joda-time" % "joda-time" % joda,
  "org.joda" % "joda-convert" % joda_convert,
  "javax.mail" % "mail" % javax_mail,
  "org.fusesource.scalate" %% "scalate-core" % scalate exclude("org.scala-lang", "scala-compiler"),
  "org.fusesource.scalamd" %% "scalamd" % scalamd,
  "commons-io" % "commons-io" % commons_io,
  "net.java.dev.jets3t" % "jets3t" % jets3t
  )
}

