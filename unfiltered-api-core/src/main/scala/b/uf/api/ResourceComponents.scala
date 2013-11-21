
package b.uf.api

import unfiltered.request._
import unfiltered.response.Html

trait ResourceAuthComponent[T] {
    def authService: AuthService[T]
    trait AuthService[T] {
        //def realm: String
        def authenticate[X](req: unfiltered.request.HttpRequest[X]): Option[T]
    }
}

object Descriptive {
    
    private val planMap = collection.mutable.Map[String, Descriptive[_]]()
    def plans[T] = planMap.asInstanceOf[collection.mutable.Map[String,Descriptive[T]]]

    private val globalQueryParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    
    private def describeGlobalQuery(paramName: String, required: Boolean, desc: =>Html) =
        globalQueryParams += ParamDescriptor(paramName, Some(htmlToStr(desc)), required)
    
    private def htmlToStr(h: Html) = h.toString.replaceAll("^Html\\(", "")
    	.replaceAll("\\)$", "").replaceAll("\\\n", "").replaceAll("\\\t", "")
    
	describeGlobalQuery("count", false, Html(
	    <p>
    		When set to <code>1</code> or <code>true</code>, this causes
    		the resource service to return a single counter object of the format:
    		<pre>
    			'count': N
    		</pre>
    		where N is an integer representing the total number of results that
    		would be returned from the query (not taking into consideration the
    		<code>page</code> and <code>size</code> pagination parameters).
    	</p>))

	describeGlobalQuery("size", false, Html(
	    <p>
    		When set to an integer > 0, the results will be limited to this value.
    	</p>))
		
	describeGlobalQuery("page", false, Html(
	    <p>
    		When <code>size</code> has also been set, this value will be used to
    		determine which offset to apply to the query results. For example, with
    		<code>size=5</code> and <code>page=3</code>, the first <code>15</code>
    		results will be skipped, and the <code>16th</code> item will be the
    		first item in the results array.
    	</p>))
		
	describeGlobalQuery("groups", false, Html(
	    <p>
    		When set, the results array will be grouped into sub-arrays of equal (or
    		as equal as possible) length. The value of this parameter determines how
    		many sub-arrays will be created.
    	</p>
	    <p>
    		Each sub-array will be part of the results array and defined under
    		the results array's item property <code>group</code>.
    	</p>))
		
	describeGlobalQuery("groupsize", false, Html(
	    <p>
    		When set, the results array will be grouped into sub-arrays where their
    		lengths will be the size of this parameter's value.
    	</p>
	    <p>
    		Each sub-array will be part of the results array and defined under
    		the results array's item property <code>group</code>.
    	</p>))
}

trait Descriptive[T] {
    this: Resource[T,_] with ResourceAuthComponent[T] =>
        
    // register self post-config
    //
    postConfig { () =>
    	Descriptive.planMap.put(FullPath, this)
    }

    def describe(ctx: Context[T]) = {        
        
        val ms = collection.mutable.ArrayBuffer[SupportedOp]()
        if (_getter.isDefinedAt(ctx))
            ms += SupportedOp("get", "GET", FullPath + "/:" + ResourceIdParam, _getterParams)
        if (_querier.isDefinedAt(ctx))
            ms += SupportedOp("query", "GET", FullPath, _querierParams ++ Descriptive.globalQueryParams)
        if (_creator.isDefinedAt(ctx))
            ms += SupportedOp("create", "POST", FullPath + " (with JSON in the request body)", _creatorParams)
        if (_rawCreator.isDefinedAt(ctx))
            ms += SupportedOp("rawCreate", "POST", FullPath + " (with custom request body and/or params required)", _rawCreatorParams)
        if (_updater.isDefinedAt(ctx))
            ms += SupportedOp("update", "PUT", FullPath + "/:" + ResourceIdParam + " (with JSON in the request body)", _updaterParams)
        if (_deleter.isDefinedAt(ctx))
            ms += SupportedOp("delete", "DELETE", FullPath + "/:" + ResourceIdParam, _deleterParams)
        val desc = _description match {
            case Some(h) => Some(_htmlToStr(h))
            case None => None
        }
        ResourceDescriptor(FullPath, version, maxPageSize, desc, ms.toSeq)
    }
    
    // allow base classes to register documentation
    //
    private def _htmlToStr(h: Html) = Descriptive.htmlToStr(h)
    
    private var _description: Option[Html] = None
    private val _getterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _querierParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _creatorParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _rawCreatorParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _updaterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _deleterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    
    def describe(desc: Html) = _description = Some(desc)    
    def describeQuery(paramName: String, required: Boolean, desc: =>Html) =
        _querierParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
    def describeCreate(paramName: String, required: Boolean, desc: =>Html) =
        _creatorParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
    def describeRawCreate(paramName: String, required: Boolean, desc: =>Html) =
        _rawCreatorParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
    def describeUpdate(paramName: String, required: Boolean, desc: =>Html) =
        _updaterParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
    def describeGet(paramName: String, required: Boolean, desc: =>Html) =
        _getterParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
    def describeDelete(paramName: String, required: Boolean, desc: =>Html) =
        _deleterParams += ParamDescriptor(paramName, Some(_htmlToStr(desc)), required)
}