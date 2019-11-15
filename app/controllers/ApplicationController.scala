package controllers

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import javax.inject.Inject
import models.User
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

  def index: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
    Future.successful(Ok(views.html.index(request.identity)))
  }

}
