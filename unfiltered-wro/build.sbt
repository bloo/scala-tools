name := "unfiltered-wro"

libraryDependencies <<= (version, scalaVersion) { (v, sv) =>
  val wro = "1.7.0"
  val sass = "3.2.1"
  val jruby = "1.7.4"
  val webjar_locator = "0.5"
  val webjar_coffeescript = "1.6.3"
  val bourbon_gem = "2.1.0"
  val rhino = "1.7R4"
  val pool = "1.6"
  Seq(
  "org.mozilla" % "rhino" % rhino,
  "commons-pool" % "commons-pool" % pool,
  "org.jruby" % "jruby-complete" % jruby,
  "me.n4u.sass" % "sass-gems" % sass, 
  "org.webjars" % "webjars-locator" % webjar_locator,
  "org.webjars" % "coffee-script" % webjar_coffeescript,
  "nz.co.edmi" % "bourbon-gem-jar" % bourbon_gem,
  "ro.isdc.wro4j" % "wro4j-core" % wro,
  "ro.isdc.wro4j" % "wro4j-extensions" % wro intransitive()
  // http://mvnrepository.com/artifact/ro.isdc.wro4j/wro4j-extensions/1.7.0
  //ExclusionRule(organization = "com.github.lltyk", name = "dojo-shrinksafe"),
  //ExclusionRule(organization = "com.github.sommeri", name = "less4j"),
  //ExclusionRule(organization = "com.google.code.gson", name = "gson"),
  //ExclusionRule(organization = "com.google.javascript", name = "closure-compiler"),
  //ExclusionRule(organization = "commons-io", name = "commons-io"),
  //ExclusionRule(organization = "commons-pool", name = "commons-pool"),
  //ExclusionRule(organization = "javax.servlet", name = "servlet-api"),
  //ExclusionRule(organization = "me.n4u.sass", name = "sass-gems"), // need newer version
  //ExclusionRule(organization = "nz.co.edmi", name = "bourbon-gem-jar"), // need
  //ExclusionRule(organization = "org.apache.commons", name = "commons-lang3"),
  //ExclusionRule(organization = "org.codehaus.gmaven.runtime", name = "gmaven-runtime-1.7"),
  //ExclusionRule(organization = "org.jruby", name = "jruby-complete"), // need jruby, but not this jar
  //ExclusionRule(organization = "org.springframework", name = "spring-web"),
  //ExclusionRule(organization = "org.webjars", name = "coffee-script"), // need
  //ExclusionRule(organization = "org.webjars", name = "emberjs"),
  //ExclusionRule(organization = "org.webjars", name = "handlebars"),
  //ExclusionRule(organization = "org.webjars", name = "jquery"),
  //ExclusionRule(organization = "org.webjars", name = "jshint"),
  //ExclusionRule(organization = "org.webjars", name = "jslint"),
  //ExclusionRule(organization = "org.webjars", name = "json2"),
  //ExclusionRule(organization = "org.webjars", name = "less"),
  //ExclusionRule(organization = "org.webjars", name = "webjars-locator") // need
  )
}

