package b.email

import javax.mail.Session

trait EmailSessionFactory {
    def session: Session
}

trait SimpleSmtpSessionFactory extends EmailSessionFactory {

	val host: String
	val port = 25
    
	lazy val props = {
	    val props = new java.util.Properties
	    props.put("mail.smtp.host", host)
	    props.put("mail.smtp.port", port.asInstanceOf[java.lang.Integer])
	    props
	}
	
    override def session: Session = {
	    Session.getInstance(props)
	}
}

trait SslSmtpSessionFactory	extends EmailSessionFactory {

	val user: String
	val password: String
    val host: String
	val port = 465

	lazy val props = {
	    val props = new java.util.Properties
	    props.put("mail.smtp.host", host)
	    props.put("mail.smtp.port", port.asInstanceOf[java.lang.Integer])
	    props.put("mail.smtp.port", port.asInstanceOf[java.lang.Integer])
	    props.put("mail.smtp.socketFactory.port", port.asInstanceOf[java.lang.Integer])
	    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
	    props.put("mail.smtp.auth", "true")	  
	    props
	}
	
    override def session: Session = {	  
		Session.getDefaultInstance(props, new javax.mail.Authenticator {
	        override def getPasswordAuthentication = {
	            new javax.mail.PasswordAuthentication(user, password)
	        }
	    })
	}
}
