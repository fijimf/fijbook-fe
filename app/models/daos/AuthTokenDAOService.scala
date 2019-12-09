package models.daos

import java.util.UUID

import akka.util.ByteString
import javax.inject.Inject
import models.AuthToken
import models._
import org.joda.time.DateTime
import play.api.{Configuration, Logger}
import play.api.libs.ws.{BodyWritable, InMemoryBody, WSClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import io.circe.parser.decode
import io.circe.syntax._


class AuthTokenDAOService @Inject() (ws:WSClient, configuration: Configuration, implicit val executionContext: ExecutionContext) extends AuthTokenDAO {
  val logger: Logger = Logger(getClass)
  val host: String = configuration.get[String]("user.host")
  val port: Int = configuration.get[Int]("user.port")

  implicit val authTokenBodyWritable: BodyWritable[AuthToken] =
    BodyWritable[AuthToken](authToken=>InMemoryBody(ByteString.fromString(authToken.asJson.noSpaces)), "text/plain")

  /**
   * Finds a token by its ID.
   *
   * @param id The unique token ID.
   * @return The found token or None if no token for the given ID could be found.
   */
  def find(id: UUID): Future[Option[AuthToken]] = {
    ws.url(s"http://$host:$port/token/${id.toString}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[Option[AuthToken]](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[Option[AuthToken]](thr)
      case Right(token) => Future.successful(token)
    }
  }

  /**
   * Finds expired tokens.
   *
   * @param dateTime The current date time.
   */
  def findExpired(dateTime: DateTime): Future[Seq[AuthToken]] = {
    ws.url(s"http://$host:$port/token/expired")
      .withQueryStringParameters(("epochMillis", dateTime.getMillis.toString))
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(resp => decode[List[AuthToken]](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[List[AuthToken]](thr)
      case Right(tokens) => Future.successful(tokens)
    }
  }

  /**
   * Saves a token.
   *
   * @param token The token to save.
   * @return The saved token.
   */
  def save(token: AuthToken): Future[AuthToken] = {
    ws.url(s"http://$host:$port/token/")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .post(token)
      .map(resp => decode[AuthToken](resp.body)).flatMap {
      case Left(thr) =>
        logger.error(s"Failed parsing AuthToken", thr)
        Future.failed[AuthToken](thr)
      case Right(token) => Future.successful(token)
    }
  }

  /**
   * Removes the token for the given ID.
   *
   * @param id The ID for which the token should be removed.
   * @return A future to wait for the process to be completed.
   */
  def remove(id: UUID):Future[Unit] = {
      ws.url(s"http://$host:$port/token/${id.toString}")
        .addHttpHeaders("Accept" -> "application/json")
        .withRequestTimeout(10000.millis)
        .delete()
        .map(resp => decode[Int](resp.body)).flatMap {
        case Left(thr) =>
          logger.error(s"Failed parsing Int", thr)
          Future.failed[Unit](thr)
        case Right(count) =>
          logger.info(s"Deleted $count AuthTokens")
          Future.successful(Unit)
      }
    }
}


