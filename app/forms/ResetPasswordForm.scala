package forms

import play.api.data.Forms._
import play.api.data._

object ResetPasswordForm {

  val form = Form(
    "password" -> nonEmptyText
  )
}
