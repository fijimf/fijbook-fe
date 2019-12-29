package jobs

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import javax.inject.Inject
import jobs.AuthTokenCleaner.Clean
import models.services.AuthTokenService
import utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A job which cleanup invalid auth tokens.
 *
 * @param service The auth token service implementation.
 * @param clock The clock implementation.
 */
class AuthTokenCleaner @Inject() (
  service: AuthTokenService,
  clock: Clock)
  extends Actor with Logger {

  def receive: Receive = {
    case Clean =>
      val start: Long = clock.now.getMillis
      logger.info("Start to cleanup auth tokens\n")
      service.clean.map { deleted =>
        val seconds: Long = (clock.now.getMillis - start) / 1000
        logger.info("Total of %s auth tokens(s) were deleted in %s seconds".format(deleted.length, seconds))
      }.recover {
        case e => logger.error("Couldn't cleanup auth tokens because of unexpected error.", e)
      }
    case _=>logger.error("Unknown message received by AuthTokenCleaner")
  }
}

object AuthTokenCleaner {
  case object Clean
}
