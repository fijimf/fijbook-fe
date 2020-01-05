package controllers

import com.fijimf.deepfij.schedule.model._
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.mixin.UserAwareOps
import io.circe.parser.decode
import javax.inject.Inject
import models.pages.TeamPage
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.libs.ws._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.api.{Configuration, Environment, Logging}
import utils.ServiceConfig
import utils.auth.DefaultEnv
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder, ObjectEncoder}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject() (
                                        ws: WSClient,
                                        cache: AsyncCacheApi,
                                        assets: Assets,
                                        components: ControllerComponents,
                                        environment: Environment,
                                        configuration: Configuration,
                                        silhouette: Silhouette[DefaultEnv],
                                        implicit val executionContext: ExecutionContext)
  extends AbstractController(components) with I18nSupport with UserAwareOps with Logging {

  val svc = ServiceConfig.load("schedule", configuration)

  def getScheduleRoot: Future[ScheduleRoot] = cache.getOrElseUpdate("schedule.root", 90.seconds) {
    ws.url(s"http://${svc.host}:${svc.port}/root")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[ScheduleRoot](resp.body)).map {
      case Left(thr) =>
        logger.error(s"Failed parsing ScheduleRoot", thr)
        ScheduleRoot(
          List.empty[Team],
          List.empty[Conference],
          List.empty[Season],
          List.empty[ConferenceMapping],
          List.empty[Game],
          List.empty[Result],
        )
      case Right(root) => root
    }
  }

  def index: Action[AnyContent] = silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
    getScheduleRoot.map(root => Ok(views.html.index(user)))
  }

  def date(yyyymmdd: Int): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
    getScheduleRoot.map(root => Ok(views.html.index(user)))
  }

  def team(key: String): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request: UserAwareRequest[DefaultEnv, AnyContent] =>
    getScheduleRoot.map(root => {
      TeamPage.create(root, key) match {
        case Some(data) =>
          Ok(views.html.team(user, data))
        case None =>
          logger.warn(s"Could not find team for $key")
          NotFound
      }
    })
  }
}
