package controllers

import java.util.UUID

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.ResetPasswordForm
import javax.inject.Inject
import models.services.{AuthTokenService, UserService}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Reset Password` controller.
 *
 * @param components            ControllerComponents
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info repository.
 * @param passwordHasherRegistry The password hasher registry.
 * @param authTokenService       The auth token service implementation.
 */
class ResetPasswordController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  passwordHasherRegistry: PasswordHasherRegistry,
  authTokenService: AuthTokenService)(
  implicit
  val ex: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  /**
   * Resets the password.
   *
   * @param token The token to identify a user.
   * @return The result to display.
   */
  def submit(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) =>
        ResetPasswordForm.form.bindFromRequest.fold(
          form => Future.successful(BadRequest(Json.obj("errors" -> form.errors.map {
            _.messages.mkString(", ")
          }))),
          password => userService.retrieve(authToken.userID).flatMap {
            case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
              val passwordInfo: PasswordInfo = passwordHasherRegistry.current.hash(password)
              authInfoRepository.update[PasswordInfo](user.loginInfo, passwordInfo).map { _ =>
                Ok
              }
            case _ => Future.successful(BadRequest(Json.obj("error" -> Messages("invalid.reset.link"))))
          }
        )
      case None => Future.successful(BadRequest(Json.obj("error" -> Messages("invalid.reset.link"))))
    }
  }

  def view(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    for {
      tok <- authTokenService.validate(token)
    } yield {
      tok match {
        case Some(_) => Ok(views.html.resetPassword(ResetPasswordForm.form, token))
        case None => Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.reset.link"))
      }
    }
  }

}
