package b.uf

package object errors {
	
    class ErrorResponse(val status: unfiltered.response.Status, code: Int, messages: String*)
    	extends b.errors.ErrorMessage(code, messages:_*)

}