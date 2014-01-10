package b.log

trait Logger {
	lazy val logger = grizzled.slf4j.Logger(getClass)
	lazy val log = logger
}