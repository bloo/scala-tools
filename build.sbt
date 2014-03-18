name := "bloo-dist"

organization in ThisBuild := "bloo"

version in ThisBuild := "1.0-SNAPSHOT"

scalaVersion in ThisBuild := "2.10.3"

resolvers in ThisBuild ++= Seq(
  Resolver.url("internal-ivy", url("http://dl.dropbox.com/u/8678549/ivy-repos"))(Resolver.ivyStylePatterns) //,
  //"internal-m2" at "http://dl.dropbox.com/u/8678549/mvn-repos"
)

publishTo in ThisBuild := Some(Resolver.file("file",  new File( "/Users/bloo/Dropbox/Public/ivy-repos" )) )

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

//javacOptions in ThisBuild ++= Seq("-Xmx1024m", "-Xms512m", "-Xss6m")

javaOptions in ThisBuild += "-Xmx2G"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked")
