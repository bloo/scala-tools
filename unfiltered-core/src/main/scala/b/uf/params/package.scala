package b.uf

import org.joda.time.format.DateTimeFormat
package object params {

    import unfiltered.request.Params

    abstract class Flag(paramName: String) extends Params.Extract(paramName, Params.first ~> {
        case Some(flag) => if ((flag equalsIgnoreCase "true") || flag == "1") Some(true) else Some(false)
        case _ => None
    })

    abstract class DatePatternParam(paramName: String, format: String) extends Params.Extract(paramName, Params.first ~> {
        case Some(dateStr) => Some(DateTimeFormat.forPattern(format).parseDateTime(dateStr))
        case _ => None
    })

    abstract class DateParam(pn: String) extends DatePatternParam(pn, "yyyy-MM-dd")
    abstract class TimeParam(pn: String) extends DatePatternParam(pn, "HH:mm:ss")
    abstract class DateTimeParam(pn: String) extends DatePatternParam(pn, "yyyy-MM-dd:HH:mm:ss")
    
    abstract class LongSeq(paramName: String) extends Params.Extract(paramName, { vals => Some(vals.map(_.toLong)) })
    abstract class IntSeq(paramName: String) extends Params.Extract(paramName, { vals => Some(vals.map(_.toInt)) })

}
