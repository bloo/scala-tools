package b.log

trait Logger {
	val logger = grizzled.slf4j.Logger(getClass)
	def log(msg: String) = logger info msg
	def info(msg: String) = logger info msg
	def debug(msg: String) = logger debug msg
	def warn(msg: String) = logger warn  msg
	def error(msg: String) = logger error msg
	def error(msg: String, t: Throwable) = logger error(msg,t)
}