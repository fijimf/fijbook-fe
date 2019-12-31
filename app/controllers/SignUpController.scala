package controllers

import java.util.UUID

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import javax.inject.Inject
import models.User
import models.services.{AuthTokenService, UserService}
import play.api.{Configuration, Logging}
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.mailer.{Email, MailerClient}
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}


class SignUpController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  authTokenService: AuthTokenService,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  configuration: Configuration,
  mailerClient: MailerClient)(implicit ec: ExecutionContext)
  extends AbstractController(components) with I18nSupport with Logging {
  /**
   * Handles the submitted JSON data.
   *
   * @return The result to display.
   */
  def submit: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
    logger.warn(s"Mailer auth user: ${configuration.get[String]("play.mailer.user")}")
    logger.warn(s"Mailer auth secret: ${configuration.get[String]("play.mailer.password")}")
    SignUpForm.form.bindFromRequest.fold(
      form=>Future.successful (BadRequest(views.html.signUp(form))),
      data => {
        val result: Result = Redirect(routes.SignUpController.view()).flashing("message" -> Messages("sign.up.email.sent", data.email))
        val loginInfo: LoginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            val url: String = routes.SignInController.view().absoluteURL()
            mailerClient.send(Email(
              subject = Messages("email.already.signed.up.subject"),
              from = Messages("email.from"),
              to = Seq(data.email),
              bodyText = Some(views.txt.emails.alreadySignedUp(user, url).body),
              bodyHtml = Some(views.html.emails.alreadySignedUp(user, url).body)
            ))

            Future.successful(result)
          case None =>
            val authInfo: PasswordInfo = passwordHasher.hash(data.password)
            val user: User = User(
              userId = UUID.randomUUID(),
              loginInfo=loginInfo,
              firstName = Some(data.firstName),
              lastName = Some(data.lastName),
              fullName = Some(data.firstName + " " + data.lastName),
              email = Some(data.email),
              avatarURL = None,
              activated = false
            )
            for {
              avatar <- avatarService.retrieveURL(data.email)
              user <- userService.save(user.copy(avatarURL = avatar))
              _ <- authInfoRepository.add(loginInfo, authInfo)
              authToken <- authTokenService.create(user.userId)
            } yield {
              val url: String = routes.ActivateAccountController.activate(authToken.id).absoluteURL()
              mailerClient.send(Email(
                subject = Messages("email.sign.up.subject"),
                from = Messages("email.from"),
                to = Seq(data.email),
                bodyText = Some(views.txt.emails.signUp(user, url).body),
                bodyHtml = Some(views.html.emails.signUp(user, url).body)
              ))

              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              result
            }
        }
      }
    )

  }

  def view: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request =>
      Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }
}
