package b.log

trait Logger {
	import grizzled.slf4j.Logger
	val logger = Logger(getClass)
}