package b.common

trait Logger {
	import grizzled.slf4j.Logger
	val logger = Logger(getClass)
}