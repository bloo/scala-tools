package b.uf.params

import unfiltered.request.Params
abstract class Flag(paramName: String) extends Params.Extract(paramName, Params.first ~> {
    case Some(flag) => if ((flag equalsIgnoreCase "true") || flag == "1") Some(true) else Some(false)
    case _ => None
})
