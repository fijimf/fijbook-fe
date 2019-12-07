package models.daos

import akka.util.ByteString
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
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


class AuthInfoDaoService @Inject()(ws: WSClient, configuration: Configuration, implicit val executionContext: ExecutionContext) extends DelegableAuthInfoDAO[PasswordInfo] {

  implicit val classTag: ClassTag[PasswordInfo]=ClassTag(PasswordInfo.getClass)

  val logger: Logger = Logger(getClass)
  val host: String = configuration.get[String]("user.host")
  val port: Int = configuration.get[Int]("user.port")

  implicit val passwordInfoBodyWritable: BodyWritable[PasswordInfo] =
    BodyWritable[PasswordInfo](passwordInfo => InMemoryBody(ByteString.fromString(passwordInfo.asJson.noSpaces)), "text/plain")


  /**
   * Finds the auth info which is linked with the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The retrieved auth info or None if no auth info could be retrieved for the given login info.
   */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    ws.url(s"http://$host:$port/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[Option[PasswordInfo]](resp.body)).flatMap {
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
    ws.url(s"http://$host:$port/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .post(authInfo)
      .map(resp => decode[PasswordInfo](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
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
    ws.url(s"http://$host:$port/authInfo/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .delete()
      .map(resp => decode[Int](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing Int", thr)
        Future.failed[Unit](thr)
      case Right(count) =>
        logger.info(s"Deleted $count AuthInfos")
        Future.successful(Unit)
    }
  }
}
