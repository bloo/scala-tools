name := "unfiltered-wro"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val wro = "1.7.0"
  val sass = "3.2.1"
  Seq(
  "me.n4u.sass" % "sass-gems" % sass, // replace wro4j-ext's older sass-gems
  "ro.isdc.wro4j" % "wro4j-core" % wro,
  "ro.isdc.wro4j" % "wro4j-extensions" % wro excludeAll(
          ExclusionRule(organization = "me.n4u.sass"),
          ExclusionRule(organization = "org.springframework"),
          ExclusionRule(organization = "com.google.javascript"),
          ExclusionRule(organization = "com.google.code.gson"),
          ExclusionRule(organization = "com.github.lltyk"),
          ExclusionRule(organization = "com.github.sommeri"),
          ExclusionRule(organization = "org.codehaus.gmaven.runtime"),
	  // just want: org.webjars.{coffee-script, webjars-locator}
          ExclusionRule(organization = "org.webjars", name = "jshint"),
          ExclusionRule(organization = "org.webjars", name = "less"),
          ExclusionRule(organization = "org.webjars", name = "emberjs"),
          ExclusionRule(organization = "org.webjars", name = "handlebars"),
          ExclusionRule(organization = "org.webjars", name = "jslint"),
          ExclusionRule(organization = "org.webjars", name = "json2")
      )
  )
}

