import sbt._
import Keys._

object BlooDistBuild extends Build {
       lazy val dist = Project(id = "bloo-dist", base = file(".")) aggregate(
       	    common,
	    slick,
       	    unfiltered_core,
	    unfiltered_api_core,
	    unfiltered_api_auth,
	    unfiltered_wro,
//	    unfiltered_wro_sbt,
	    specs2_wro,
	    specs2_core,
	    specs2_api,
	    specs2_slick)

       lazy val common              = Project(id = "common", base = file("common"))

       lazy val slick               = Project(id = "slick", base = file("slick")) dependsOn(common, specs2_core)

       lazy val unfiltered_core     = Project(id = "unfiltered-core", base = file("unfiltered-core")) dependsOn(common, specs2_core)
       lazy val unfiltered_api_core = Project(id = "unfiltered-api-core", base = file("unfiltered-api-core")) dependsOn(unfiltered_core)
       lazy val unfiltered_api_auth = Project(id = "unfiltered-api-auth", base = file("unfiltered-api-auth")) dependsOn(unfiltered_api_core) //, specs2_api) // need our own specs2-api to run tests
       lazy val unfiltered_wro      = Project(id = "unfiltered-wro", base = file("unfiltered-wro")) dependsOn(unfiltered_core)
       lazy val unfiltered_wro_sbt  = Project(id = "unfiltered-wro-sbt", base = file("unfiltered-wro-sbt")) dependsOn(unfiltered_wro)

       lazy val specs2_core         = Project(id = "specs2-core", base = file("specs2-core")) dependsOn(common)
       lazy val specs2_wro          = Project(id = "specs2-wro", base = file("specs2-wro")) dependsOn(unfiltered_wro)
       lazy val specs2_api          = Project(id = "specs2-api", base = file("specs2-api")) dependsOn(unfiltered_api_auth, specs2_core)
       lazy val specs2_slick        = Project(id = "specs2-slick", base = file("specs2-slick")) dependsOn(slick, specs2_core)
}
