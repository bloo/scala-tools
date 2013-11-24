package b.uf.api

import unfiltered.response.Html

case class ParamDescriptor(
        val name: String,
        val description: Option[String],
        val required: Boolean)

case class SupportedOp(
        val name: String,
        val httpMethod: String,
        val usage: String,
        val params: Seq[ParamDescriptor])
        
case class ResourceDescriptor(
        val path: String,
        val version: Double,
        val maxPageSize: Option[Int],
        val description: Option[String],
        val operations: Seq[SupportedOp])

class ResourceDescriptionsResource[T]
	extends Resource[T,ResourceDescriptor]("resources")
	with Descriptive[T] {
    this: ResourceAuthComponent[T] =>
    
    override def resolve = {
        case (ctx,path) => Descriptive.plans[T].get(path) match {
        	case Some(plan) => Some(plan.describe(ctx))
        	case None => None
        }
    }
    
    get { case (ctx) => Some(_) }
    
    describe(Html(
        <p>
    		This resource tracks the registrations and descriptions of the other
    		ResourcePlans so that they can be displayed to the end user.
    	</p>
    ))
    
    query { case (ctx) => { qp =>
        	val descs = Descriptive.plans[T].values.map {
        	    case (plan) => plan.describe(ctx)
        	}
        	
        	// TODO ordering
        	//
        	(qp.size match {
        	    case Some(sz) if sz > 0 => qp.page match {
        	        case Some(pg) if pg > 0 => descs.drop(pg*sz).take(sz)
        	        case None => descs.take(sz)
        	    }
        	    case None => descs
        	}).toSeq.sortBy(_.path).reverse
    	}
    }
    
    count { case (_) => { () => Descriptive.plans[T].size }}
}
