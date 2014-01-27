
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
    
    private def describeGlobalQuery(paramName: String, required: Boolean, desc: xml.NodeSeq) =
        globalQueryParams += ParamDescriptor(paramName, Some(desc.toString), required)
    
	describeGlobalQuery("count", false,
	    <p>
    		When set to <code>1</code> or <code>true</code>, this causes
    		the resource service to return a single counter object of the format:
    		<pre>
    			'count': N
    		</pre>
    		where N is an integer representing the total number of results that
    		would be returned from the query (not taking into consideration the
    		<code>page</code> and <code>size</code> pagination parameters).
    	</p>)

	describeGlobalQuery("size", false,
	    <p>
    		When set to an integer > 0, the results will be limited to this value.
    	</p>)
		
	describeGlobalQuery("page", false,
	    <p>
    		When <code>size</code> has also been set, this value will be used to
    		determine which offset to apply to the query results. For example, with
    		<code>size=5</code> and <code>page=3</code>, the first <code>15</code>
    		results will be skipped, and the <code>16th</code> item will be the
    		first item in the results array.
    	</p>)
		
	describeGlobalQuery("groups", false,
	    <p>
    		When set, the results array will be grouped into sub-arrays of equal (or
    		as equal as possible) length. The value of this parameter determines how
    		many sub-arrays will be created.
    	</p>
	    <p>
    		Each sub-array will be part of the results array and defined under
    		the results array's item property <code>group</code>.
    	</p>
	    <p>
    		If <code>group</code> and <code>groupsize</code> are defined,
			<code>groupsize</code> is ignored.
    	</p>)
		
	describeGlobalQuery("groupsize", false,
	    <p>
    		When set, the results array will be grouped into sub-arrays where their
    		lengths will be the size of this parameter's value.
    	</p>
	    <p>
    		Each sub-array will be part of the results array and defined under
    		the results array's item property <code>group</code>.
    	</p>
	    <p>
    		If <code>group</code> and <code>groupsize</code> are defined,
			<code>groupsize</code> is ignored.
    	</p>)
		
	describeGlobalQuery("grouptranspose", false,
	    <p>
    		When set, along with <code>groups</code> or <code>groupsize</code>,
			the results array will be grouped into sub-arrays accordingly, but
			transposed.
    	</p>
	    <p>
			For example, usually, if <code>groupsize=3</code>, the first 3 result
			items would be placed into the first group, whereas if the total result
			set length was 9 and <code>grouptranspose=1</code>, the first, 4th, and 7th
			items would be placed into the first group.
    	</p>)
}

trait Descriptive[T] {
    this: Resource[T,_] with ResourceAuthComponent[T] =>
    
    // register self post-config
    //
    postConfig { () =>
    	Descriptive.planMap.put(pathToId(FullPath), this)
    }

    def describe(ctx: Context[T]) = {        
        
        val ms = collection.mutable.ArrayBuffer[SupportedOp]()
        if (_getter isDefinedAt ctx)
            ms += SupportedOp("get", "GET", FullPath + "/:" + ResourceIdParam, _getterParams)
        if (_querier isDefinedAt ctx)
            ms += SupportedOp("query", "GET", FullPath, _querierParams ++ Descriptive.globalQueryParams)
        if (_creator isDefinedAt ctx)
            ms += SupportedOp("create", "POST", FullPath + " (with JSON in the request body)", _creatorParams)
        if (_updater isDefinedAt ctx)
            ms += SupportedOp("update", "PUT", FullPath + "/:" + ResourceIdParam + " (with JSON in the request body)", _updaterParams)
        if (_deleter isDefinedAt ctx)
            ms += SupportedOp("delete", "DELETE", FullPath + "/:" + ResourceIdParam, _deleterParams)
        val desc = _description match {
            case Some(h) => Some(h.toString)
            case None => None
        }
        ResourceDescriptor(pathToId(FullPath), FullPath, version, maxPageSize, desc, ms.toSeq)
    }
    
    def pathToId(path: String) = path.replaceAll("^/+", "").replaceAll("/", "_")
    
    private var _description: Option[xml.Elem] = None
    private val _getterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _querierParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _creatorParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _updaterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    private val _deleterParams = collection.mutable.ArrayBuffer[ParamDescriptor]()
    
    def describe(desc: xml.Elem) = _description = Some(desc)    
    
    private def describeQuery(paramName: String, required: Boolean, desc: xml.Elem) =
        _querierParams += ParamDescriptor(paramName, Some(desc.toString), required)
    private def describeCreate(paramName: String, required: Boolean, desc: xml.Elem) =
        _creatorParams += ParamDescriptor(paramName, Some(desc.toString), required)
    private def describeUpdate(paramName: String, required: Boolean, desc: xml.Elem) =
        _updaterParams += ParamDescriptor(paramName, Some(desc.toString), required)
    private def describeGet(paramName: String, required: Boolean, desc: xml.Elem) =
        _getterParams += ParamDescriptor(paramName, Some(desc.toString), required)
    private def describeDelete(paramName: String, required: Boolean, desc: xml.Elem) =
        _deleterParams += ParamDescriptor(paramName, Some(desc.toString), required)
        
    trait DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
    	def describe: (String, Boolean, xml.Elem)
        protected val d = describe
    }

    trait DescribesQuery extends DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
        describeQuery(d._1, d._2, d._3)
    }

    trait DescribesCreate extends DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
        describeCreate(d._1, d._2, d._3)
    }

    trait DescribesUpdate extends DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
        describeUpdate(d._1, d._2, d._3)
    }
    
    trait DescribesGet extends DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
        describeGet(d._1, d._2, d._3)
    }

    trait DescribesDelete extends DescribesBase {
        this: unfiltered.request.Params.Extract[_,_] =>
        describeDelete(describe._1, describe._2, describe._3)
    }
}