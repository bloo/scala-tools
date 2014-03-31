package b.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Conf {

	def apply(base: Option[String] = None) = {
		
	}
}

class Conf(base: Option[String] = None) {
	
	private var _cfg: Option[Config] = None
	
	def config(c: Config) = _cfg = Some(base match {
		case Some(b) => c getConfig b
		case None => c
	})
	
	def get:Config = _cfg match {
		case Some(c) => c
		case None => {
			config(ConfigFactory.load())
			get
		}
	}
}