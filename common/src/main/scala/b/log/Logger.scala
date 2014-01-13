package b.log

trait Logger {
	val logger = grizzled.slf4j.Logger(getClass)
	val log = logger
}