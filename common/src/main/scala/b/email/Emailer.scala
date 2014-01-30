package b.email

import b.scalate._
import java.util.Properties

case class EmailAdr(
    email: String,
    name: Option[String] = None)

object EmailAdr {
	def apply(email: String, fn: String, ln: String): EmailAdr = {
		EmailAdr(email, Some("%s %s" format (fn, ln)))
	}
}

case class MessageContext(
    template: String,
    subject: String,
    from: EmailAdr,
    replyTo: Option[EmailAdr] = None)

object ScalateEmailer {
    private var cfg: Option[ScalateEngine] = None
    def config(debug: Boolean = false, engine: ScalateEngine = new ScalateEngine(".jade", "scalate/email", "layouts/default.jade")) = {
        engine setDebug debug
        ScalateEmailer.cfg = Some(engine)
    }
}

trait ScalateEmailer {
    def render = ScalateEmailer.cfg.get.render _ 
    def templateExists = ScalateEmailer.cfg.get.templateExists _
}

trait Emailer extends ScalateEmailer {
    this: EmailSessionFactory =>

    var subjectPrefix: Option[String] = None
    
	import javax.mail.internet.InternetAddress

	implicit def msgrToInetAdr(msg: EmailAdr): InternetAddress = msg.name match {
	    case Some(name) => new InternetAddress(msg.email, name)
	    case None => new InternetAddress(msg.email)
	}
	
	class EmailMessageBuilder(ctx: MessageContext, attributes: (String, Any)*) {
		
		import javax.mail.Message
	    import javax.mail.internet.MimeMessage

		val mime = new MimeMessage(session)
		mime setFrom ctx.from
		ctx.replyTo map { rt => mime setReplyTo(Array(rt)) }
		subject(ctx.subject)
			        
        import javax.mail.BodyPart
        import javax.mail.internet.MimeMultipart
        import javax.mail.internet.MimeBodyPart
        import java.io.StringWriter
        import javax.mail.Transport

        val mp = new MimeMultipart("alternative")

        // build html content from scalate template
        //
        val html = new MimeBodyPart()
        val htmlWriter = new StringWriter()
        render(ctx.template, htmlWriter, attributes)
        htmlWriter.close()
        html setContent(htmlWriter.toString(), "text/html")
        mp addBodyPart html

        // build plain text content from scalate template
        //
        val textTmpl = ctx.template + ".txt"
        if (templateExists(textTmpl)) {
	        val text = new MimeBodyPart()
	        val textWriter = new StringWriter()
	        render(textTmpl, textWriter, attributes)
	        textWriter.close()
	        text setText(textWriter.toString())
	        mp addBodyPart text
        }
        
        // combine parts
        //
        mime setContent mp
		
        def subject(subj: String) = {
		    val s = subjectPrefix match {
		      case Some(pre) => "%s %s" format (pre, subj)
		      case None => subj
		    }
		    mime setSubject s
		    this
        }
        
		def cc(cc: EmailAdr) = recipient(Message.RecipientType.CC, cc)

		def bcc(bcc: EmailAdr) = recipient(Message.RecipientType.BCC, bcc)

		def to(to: EmailAdr) = recipient(Message.RecipientType.TO, to)
		
		private def recipient(rt: Message.RecipientType, ea: EmailAdr) = {
	    	mime.setRecipient(rt, ea)
	    	this
	    }

	    def send(): Unit = Transport send mime
	}
	
    def compose(ctx: MessageContext, attributes: (String, Any)*): EmailMessageBuilder =
        new EmailMessageBuilder(ctx, attributes:_*)
}


