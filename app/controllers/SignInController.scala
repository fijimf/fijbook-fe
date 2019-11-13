package controllers

import javax.inject.Inject
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import com.typesafe.config.Config
import forms.SignInForm
import models.services.UserService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request, Result}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * The `Sign In` controller.
 *
 * @param components The ControllerComponents.
 * @param silhouette The Silhouette stack.
 * @param userService The user service implementation.
 * @param credentialsProvider The credentials provider.
 * @param configuration The Play configuration.
 * @param clock The clock instance.
 */
class SignInController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  credentialsProvider: CredentialsProvider,
  configuration: Configuration,
  clock: Clock)(implicit ec: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(form => formErrors(form), data => success(data))
  }

  def formErrors(form: Form[SignInForm.Data])(implicit req: Request[AnyContent]): Future[Result] =
   Future.successful{
    BadRequest(views.html.signIn( form))
  }

  def success(data:SignInForm.Data)(implicit req: Request[AnyContent]): Future[Result] = {
    val credentials = Credentials(data.email, data.password)
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      val result: Result = Redirect(routes.ApplicationController.index())
      userService.retrieve(loginInfo).flatMap {
        case Some(user) if !user.activated =>
            Future.successful(Ok(views.html.activateAccount(data.email)))
        case Some(user) =>
          val c: Config = configuration.underlying
          silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator if data.rememberMe =>
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, req))
            silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
              silhouette.env.authenticatorService.embed(v, result)
            }
          }
        case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
      }
    }.recover {
      case _: ProviderException =>
        Redirect(routes.SignInController.view()).flashing("message"->Messages("invalid.credentials"))
    }
  }

  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signIn(SignInForm.form)))
  }


}
