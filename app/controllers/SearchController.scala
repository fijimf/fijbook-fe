package controllers

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import models.services.UserService
import play.api.cache.AsyncCacheApi
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import utils.auth.DefaultEnv

import scala.concurrent.{ExecutionContext, Future}

class SearchController @Inject()(
                                  val controllerComponents: ControllerComponents,
                                  val cache: AsyncCacheApi,
                                  val userService: UserService,
                                  val silhouette: Silhouette[DefaultEnv])(implicit ec: ExecutionContext)
  extends BaseController with I18nSupport {

  def search(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit rs =>
    val qs: Seq[String] = rs.request.queryString.getOrElse("q", Seq.empty[String])

    Future.successful(Ok(s"OK ${qs.mkString(", ")}"))
  }
}




