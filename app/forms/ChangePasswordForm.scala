package forms

import play.api.data.Forms._
import play.api.data._

object ChangePasswordForm {

  val form = Form(tuple(
    "current-password" -> nonEmptyText,
    "new-password" -> nonEmptyText
  ))

  case class Data(
    currentPassword: String,
    newPassword: String)
}
