package controllers

import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import javax.inject.Inject
import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.libs.ws._
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject() (
  ws: WSClient,
  assets: Assets,
  components: ControllerComponents,
  environment: Environment,
  silhouette: Silhouette[DefaultEnv],
  implicit val executionContext: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  def signOut: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Ok)
  }

  def index: Action[AnyContent] = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.index()))
  }

}
