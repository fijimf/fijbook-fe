package utils

import java.time.LocalDateTime

import cats.implicits._
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


case class ServiceConfig(key: String, host: String, port: Int, statusEndpoint: String, info: Either[Throwable, ServerInfo], asOf: LocalDateTime) {
  implicit val serverInfoDecoder: Decoder[ServerInfo] = deriveDecoder[ServerInfo]
  val logger: Logger = Logger(classOf[ServiceConfig])

  override def toString: String = s"http://$host:$port"

  val name: String = info.fold(_ => "-", _.name)
  val version: String = info.fold(_ => "-", _.version)
  val scalaVersion: String = info.fold(_ => "-", _.scalaVersion)
  val sbtVersion: String = info.fold(_ => "-", _.sbtVersion)
  val buildNumber: Any = info.fold(_ => "-", _.buildNumber)
  val builtAt: String = info.fold(_ => "-", _.builtAt)
  val isOk: Boolean = info.fold(_ => false, _.isOk)

  def refresh(config: Configuration, ws: WSClient): Future[ServiceConfig] = {
    val svc: ServiceConfig = ServiceConfig.load(key, config)
    ws.url(s"$svc${svc.statusEndpoint}")
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
      .get()
      .map(handleResponse)
      .map(e => svc.copy(info = e, asOf = LocalDateTime.now()))
  }

  def handleResponse(resp: WSResponse): Either[Throwable, ServerInfo] = {
    resp.status match {
      case 200 =>
        decode[ServerInfo](resp.body)
      case _ =>
        Either.left[Throwable, ServerInfo](new RuntimeException("Unexpected return code"))
    }
  }
}

object ServiceConfig {


  def load(key: String, config: Configuration): ServiceConfig = {
    val host: String = config.get[String](s"deepfij.services.$key.host")
    val port: Int = config.get[Int](s"deepfij.services.$key.port")
    val statusEndpoint: String = config.get[String](s"deepfij.services.$key.statusEndpoint")

    ServiceConfig(key, host, port, statusEndpoint, Either.left[Throwable, ServerInfo](new IllegalStateException("Service not yet initialized")), LocalDateTime.now())
  }

  def loadAll(config: Configuration): Map[String, ServiceConfig] = {
    config.get[Configuration]("deepfij.services").subKeys.map(k =>
      k -> load(k, config)
    ).toMap
  }

  def refreshAll(config: Configuration, ws: WSClient) = Future.sequence(loadAll(config).values.map(_.refresh(config, ws)))
}
