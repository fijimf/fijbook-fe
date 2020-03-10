package controllers

import org.apache.commons.mail.DefaultAuthenticator

object JunkMail {
  def main(args: Array[String]): Unit = {
    import org.apache.commons.mail.Email
    import org.apache.commons.mail.SimpleEmail
    val email = new SimpleEmail
    email.setHostName("email-smtp.us-east-1.amazonaws.com")
    email.setSmtpPort(587)
    email.setAuthenticator(new DefaultAuthenticator("", ""))
    email.setStartTLSRequired(true)
    email.setSSLOnConnect(true)
    email.setFrom("deepfij@gmail.com")
    email.setSubject("TestMail")
    email.setMsg("This is a test mail ... :-)")
    email.addTo("fijimf@gmail.com")
    email.send()
  }
}
