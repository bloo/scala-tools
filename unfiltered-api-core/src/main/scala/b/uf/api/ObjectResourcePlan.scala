package b.uf.api

object ObjectStatus extends Enumeration {
    type ObjectStatus = Value
    val ready, uploading, completed, error, unknown = Value
}

import ObjectStatus._
case class ObjectMeta(val id: String, val name: String, val url: String,
        val size: Int, val error: Option[String])

trait ObjectComponent {

    val objectService: ObjectService
    
    trait ObjectService {
        
    }
}


abstract class ObjectResourcePlan[T](Version: Int, Group: String, ResourcePath: String, MaxPageSize: Option[Int] = None)
	extends ResourcePlan[T,ObjectMeta](Version,Group,ResourcePath,MaxPageSize) {
	this: ResourceAuthComponent[T] with ObjectComponent =>
	    
	    
	    
}