name := "specs2-slick"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val postgres = "9.1-901-1.jdbc4"
  Seq(
    "postgresql" % "postgresql" % postgres % "test"
  )
}

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.withSource := true

sourceManaged in Compile <<= (sourceDirectory in Compile)(_ / "resources")

sourceManaged in Test <<= (sourceDirectory in Test)(_ / "resources")
