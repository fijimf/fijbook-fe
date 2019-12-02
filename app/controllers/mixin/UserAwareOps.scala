package controllers.mixin

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import models.User
import play.api.mvc.AnyContent
import utils.auth.DefaultEnv

trait UserAwareOps {
   def user(implicit request: UserAwareRequest[DefaultEnv, AnyContent]):Option[User] = request.identity
}
