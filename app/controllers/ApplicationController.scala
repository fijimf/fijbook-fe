package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import play.api.{ Environment, Mode }
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import utils.auth.DefaultEnv
import play.api.libs.ws._

import scala.concurrent.{ ExecutionContext, Future }

/**
 * The basic application controller.
 *
 * @param components The ControllerComponents.
 * @param environment Environment.
 * @param silhouette The Silhouette stack.
 */
class ApplicationController @Inject() (
  ws: WSClient,
  assets: Assets,
  components: ControllerComponents,
  environment: Environment,
  silhouette: Silhouette[DefaultEnv],
  implicit val executionContext: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  /**
   * Returns the user.
   *
   * @return The result to display.
   */

  def user = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(Json.toJson(request.identity)))
  }

  /**
   * Manages the sign out action.
   */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Ok)
  }

  /*
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.index()))
  }

}
