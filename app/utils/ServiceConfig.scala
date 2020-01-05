package utils

import play.api.Configuration

case class ServiceConfig(host: String, port: Int, statusEndpoint: String) {

  /**
   * Watch out for funky toString
   */
  override def toString: String = s"http://$host:$port"
}

object ServiceConfig {
  def load(key: String, config: Configuration): ServiceConfig = {
   val host: String = config.get[String](s"deepfij.services.$key.host")
    val port: Int = config.get[Int](s"deepfij.services.$key.port")
    val statusEndpoint: String = config.get[String](s"deepfij.services.$key.statusEndpoint")
    ServiceConfig(host, port, statusEndpoint)
  }

  def loadAll(config: Configuration): Map[String, ServiceConfig] = {
    config.get[Configuration]("deepfij.services").subKeys.map(k =>
      k -> load(k, config)
    ).toMap
  }
}
