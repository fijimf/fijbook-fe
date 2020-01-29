package controllers

import cats.implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.providers._
import controllers.mixin.UserAwareOps
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser._
import javax.inject.Inject
import models.services.UserService
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Logger}
import utils.{ServerInfo, ServiceConfig}
import utils.auth.DefaultEnv

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * The `Sign In` controller.
 *
 * @param components          The ControllerComponents.
 * @param silhouette          The Silhouette stack.
 * @param userService         The user service implementation.
 * @param credentialsProvider The credentials provider.
 * @param configuration       The Play configuration.
 * @param clock               The clock instance.
 */
class ServerStatusController @Inject()(
                                        components: ControllerComponents,
                                        ws: WSClient,
                                        silhouette: Silhouette[DefaultEnv],
                                        userService: UserService,
                                        credentialsProvider: CredentialsProvider,
                                        configuration: Configuration,
                                        clock: Clock)(implicit ec: ExecutionContext)
  extends AbstractController(components) with I18nSupport with UserAwareOps {
  val logger: Logger = Logger(getClass)

//  case class ServerInfo(name: String, version: String, scalaVersion: String, sbtVersion: String, buildNumber: Int, builtAt: String, isOk: Boolean)

  implicit val serverInfoDecoder = deriveDecoder[ServerInfo]

  def view: Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    ServiceConfig.refreshAll(configuration,ws).map(lst=>{
      Ok(views.html.services(lst.toList.sortBy(_.key), request.identity))
    })
  }
}
