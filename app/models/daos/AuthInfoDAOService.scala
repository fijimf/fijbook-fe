package models.daos

import cats.implicits._
import akka.util.ByteString
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import utils.ServiceConfig
//import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import io.circe.parser.decode
import io.circe.syntax._
import javax.inject.Inject
import models._
import play.api.libs.ws.{BodyWritable, InMemoryBody, WSClient}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


class AuthInfoDAOService @Inject()(ws: WSClient, configuration: Configuration, implicit val executionContext: ExecutionContext) extends DelegableAuthInfoDAO[PasswordInfo] {

  implicit val classTag: ClassTag[PasswordInfo]=ClassTag(classOf[PasswordInfo])

  val logger: Logger = Logger(getClass)
  val svc: ServiceConfig = ServiceConfig.load("user", configuration)
  implicit val passwordInfoBodyWritable: BodyWritable[PasswordInfo] =
    BodyWritable[PasswordInfo](passwordInfo => InMemoryBody(ByteString.fromString(passwordInfo.asJson.noSpaces)), "application/json")

  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    val requestString = s"$svc/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}"
    ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[Option[PasswordInfo]](resp.body)
        case 404 =>
          logger.info(s"No password info found for request $requestString")
          Either.right[Error, Option[PasswordInfo]](Option.empty[PasswordInfo])
        case r =>
          logger.warn(s"GET $requestString returned status $r")
          Either.right[Error, Option[PasswordInfo]](Option.empty[PasswordInfo])
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[Option[PasswordInfo]](thr)
      case Right(user) => Future.successful(user)
    }
  }

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo  The auth info to add.
   * @return The added auth info.
   */
  def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val requestString = s"$svc/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}"
    ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .post(authInfo)

      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[PasswordInfo](resp.body)
        case r =>
          logger.warn(s"POST $requestString returned status $r")
          Either.left[Throwable, PasswordInfo](new RuntimeException(s"$requestString"))
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing PasswordInfo", thr)
        Future.failed[PasswordInfo](thr)
      case Right(user) => Future.successful(user)
    }
  }


  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo  The auth info to update.
   * @return The updated auth info.
   */
  def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = add(loginInfo, authInfo)

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo  The auth info to save.
   * @return The saved auth info.
   */
  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = add(loginInfo, authInfo)

  /**
   * Removes the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(loginInfo: LoginInfo): Future[Unit] = {
    val requestString = s"$svc/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}"
    ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .delete()

      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[Int](resp.body)
        case r =>
          logger.warn(s"DELETE $requestString returned status $r")
          Either.right[Error, Int](0)
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing Int", thr)
        Future.failed[Unit](thr)
      case Right(count) =>
        logger.info(s"Deleted $count AuthInfos")
        Future.successful(Unit)
    }
  }
}
