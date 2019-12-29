package controllers

import java.net.URLDecoder
import java.util.UUID

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.Inject
import models.services.{AuthTokenService, UserService}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Result}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ActivateAccountController @Inject()(
                                           components: ControllerComponents,
                                           silhouette: Silhouette[DefaultEnv],
                                           userService: UserService,
                                           authTokenService: AuthTokenService,
                                           mailerClient: MailerClient)(implicit ec: ExecutionContext)
  extends AbstractController(components) with  I18nSupport {

  def send(email: String): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    val decodedEmail: String = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    userService.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenService.create(user.userID).map { authToken =>
          val url: String = routes.ActivateAccountController.activate(authToken.id).absoluteURL()

          mailerClient.send(Email(
            subject = Messages("email.activate.account.subject"),
            from = Messages("email.from"),
            to = Seq(decodedEmail),
            bodyText = Some(views.txt.emails.activateAccount(user, url).body),
            bodyHtml = Some(views.html.emails.activateAccount(user, url).body)
          ))
          Redirect(routes.SignInController.view()).flashing("info" -> Messages("activation.email.sent", decodedEmail))
        }
      case None => Future.successful(Redirect(routes.SignInController.view()).flashing("info" -> Messages("activation.email.sent", decodedEmail)))
    }
  }

  def activate(token: UUID): Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    authTokenService.validate(token).flatMap {
      case Some(authToken) => userService.retrieve(authToken.userId).flatMap {
        case Some(user) if user.providerId == CredentialsProvider.ID =>
          userService.save(user.copy(activated = true)).map { _ =>
            Redirect(routes.SignInController.view()).flashing("success" -> Messages("account.activated"))
          }
        case _ => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
      }
      case None => Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.activation.link")))
    }
  }
}