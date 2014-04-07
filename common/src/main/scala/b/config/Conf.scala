package b.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

trait Conf {
	
	val configRoot: Option[String]
	private var _cfg: Option[Config] = None

	def config(c: Config) = _cfg = Some(c)

	def cfg: Config = _cfg match {
		case Some(c) => configRoot match {
			case Some(root) => c getConfig root
			case None => c
		}
		case None => {
			config(ConfigFactory.load())
			cfg
		}
	}
}
