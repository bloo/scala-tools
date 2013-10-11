package b

package object errors {
	
	type ErrorMessage = ErrorMgr.Message
    
    object ErrorMgr extends b.common.Logger {
        val codes = scala.collection.mutable.ListBuffer[Int]()
	    class Message(val code: Int, val messages: String*) {
	        if (codes.contains(code))
	            logger.warn("Duplicate error code: %d" format code)
	        else
	            codes += code
	    }
    }
    
}