package b.email

import javax.mail.Session

trait EmailSessionFactory {
    def session: Session
}

class SimpleSmtpSessionFactory(
        host: String,
        port: java.lang.Integer = 25)
	extends EmailSessionFactory {

    val props = new java.util.Properties
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    
    override def session: Session = Session.getInstance(props)
}

class SslSmtpSessionFactory(
        user: String,
        password: String,
        host: String,
        port: java.lang.Integer = 465)
	extends EmailSessionFactory {
    
    val props = new java.util.Properties
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.socketFactory.port", port)
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
    props.put("mail.smtp.auth", "true")

    override def session: Session = Session.getDefaultInstance(props, new javax.mail.Authenticator {
        override def getPasswordAuthentication = {
            new javax.mail.PasswordAuthentication(user, password)
        }
    })
}
