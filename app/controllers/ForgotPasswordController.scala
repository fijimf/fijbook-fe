package controllers

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.ForgotPasswordForm
import javax.inject.Inject
import models.services.{AuthTokenService, UserService}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Json
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Forgot Password` controller.
 *
 * @param components       ControllerComponents
 * @param silhouette       The Silhouette stack.
 * @param userService      The user service implementation.
 * @param authTokenService The auth token service implementation.
 * @param mailerClient     The mailer client.
 */
class ForgotPasswordController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authTokenService: AuthTokenService,
  mailerClient: MailerClient
)(
  implicit
  val ex: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  /**
   * Sends an email with password reset instructions.
   *
   * It sends an email to the given address if it exists in the database. Otherwise we do not show the user
   * a notice for not existing email addresses to prevent the leak of existing email addresses.
   *
   * @return The result to display.
   */
  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    ForgotPasswordForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.forgotPassword(form))),
      email => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) if user.email.isDefined =>
            authTokenService.create(user.userId).map { authToken =>
              val url = routes.ResetPasswordController.view(authToken.id).absoluteURL()

              mailerClient.send(Email(
                subject = Messages("email.reset.password.subject"),
                from = Messages("email.from"),
                to = Seq(email),
                bodyText = Some(views.txt.emails.resetPassword(user, url).body),
                bodyHtml = Some(views.html.emails.resetPassword(user, url).body)
              ))
              Redirect(routes.SignInController.view()).flashing("message"->Messages("reset.email.sent"))
            }
          case _ => Future.successful(Redirect(routes.SignInController.view()).flashing("message"->Messages("reset.email.sent")))
        }
      }
    )
  }

  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.forgotPassword( ForgotPasswordForm.form)))
  }

}
