package models.daos

import java.util.UUID

import akka.util.ByteString
import cats.implicits._
import com.mohiva.play.silhouette.api.LoginInfo
import io.circe.{Json, Printer}
import io.circe.parser.decode
import io.circe.syntax._
import javax.inject.Inject
import models.{User, _}
import play.api.libs.ws.{BodyWritable, InMemoryBody, JsonBodyWritables, WSClient, WSRequest}
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

  implicit val circeJsonBodyWritable: BodyWritable[Json] =
    BodyWritable[Json](json => InMemoryBody(ByteString.fromString(Printer.noSpaces.print(json))), "application/json")

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo): Future[Option[User]] = {
    val requestString = s"http://$host:$port/user/${loginInfo.providerID.toString}/${loginInfo.providerKey.toString}"
    ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[Option[User]](resp.body)
        case 404 =>
          logger.info(s"No password info found for request $requestString")
          Either.right[Error, Option[User]](Option.empty[User])
        case r =>
          logger.warn(s"GET $requestString returned status $r")
          Either.right[Error, Option[User]](Option.empty[User])
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing User", thr)
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
    val requestString = s"http://$host:$port/user/${userID.toString}"
    ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[Option[User]](resp.body)
        case 404 =>
          logger.info(s"No password info found for request $requestString")
          Either.right[Error, Option[User]](Option.empty[User])
        case r =>
          logger.warn(s"GET $requestString returned status $r")
          Either.right[Error, Option[User]](Option.empty[User])
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing User", thr)
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
    val requestString = s"http://$host:$port/user"
    val request: WSRequest = ws.url(requestString)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .withBody(user.asJson)
    logger.warn(request.toString)
    logger.warn(request.body.toString)
    request.execute("POST")
//      .post(user)
      .map(resp => resp.status match {
        case 200 =>
          logger.info(resp.body)
          decode[User](resp.body)
        case r =>
          logger.warn(s"POST $requestString returned status $r")
          logger.warn(resp.body)
          Either.left[Throwable, User](new RuntimeException(s"POST $requestString"))
      }).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing User", thr)
        Future.failed[User](thr)
      case Right(user) => Future.successful(user)
    }
  }
}


