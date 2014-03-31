import sbt._
import Keys._
import java.io.File
import ro.isdc.wro.http.WroFilter

object Plugin extends sbt.Plugin {

	object WroKeys {
		import b.uf.wro.WroPlans._

		val wroConfigs = taskKey[Seq[(String, WroFilter)]]("Paths -> WroFilter/UFPlan mapping")
		val wroGen = taskKey[Seq[File]]("Generate the WRO resources")

		wroConfigs := Seq(
			"/rsrc/www.raw"  -> aggOnlyPlan(resourceDirectory.value / "wro-www.xml"),
			"/rsrc/www"      -> appPlan(resourceDirectory.value / "wro-www.xml"),
			"/rsrc/libs.raw" -> aggOnlyPlan(resourceDirectory.value / "wro-libs.xml"),
			"/rsrc/libs"     -> libAppPlan(resourceDirectory.value / "wro-libs.xml"))
		wroGen := b.uf.wro.WroGenerator.generate(target.value, wroConfigs.value:_*)
	}
}
