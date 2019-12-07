package models.daos

import java.util.UUID

import akka.util.ByteString
import com.mohiva.play.silhouette.api.LoginInfo
import io.circe.parser.decode
import io.circe.syntax._
import javax.inject.Inject
import models.{User, _}
import play.api.libs.ws.{BodyWritable, InMemoryBody, WSClient}
import play.api.{Configuration, Logger}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Give access to the user object.
 */
class UserDAOService @Inject()(ws: WSClient, configuration: Configuration, implicit val executionContext: ExecutionContext) extends UserDAO {
  val logger: Logger = Logger(getClass)
  val host: String = configuration.get[String]("user.host")
  val port: Int = configuration.get[Int]("user.port")

  implicit val userBodyWritable: BodyWritable[User] =
    BodyWritable[User](user => InMemoryBody(ByteString.fromString(user.asJson.noSpaces)), "text/plain")

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    ws.url(s"http://$host:$port/user/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[Option[User]](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[Option[User]](thr)
      case Right(user) => Future.successful(user)
    }
  }

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID): Future[Option[User]] = {
    ws.url(s"http://$host:$port/user/${userID.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[Option[User]](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[Option[User]](thr)
      case Right(user) => Future.successful(user)
    }
  }


  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User): Future[User] = {
    ws.url(s"http://$host:$port/user/")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .post(user)
      .map(resp => decode[User](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[User](thr)
      case Right(user) => Future.successful(user)
    }
  }
}


