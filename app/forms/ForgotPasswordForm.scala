package forms

import play.api.data.Forms._
import play.api.data._

object ForgotPasswordForm {
  val form = Form(
    "email" -> email
  )
}
