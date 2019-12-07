package modules

import com.google.inject.AbstractModule
import models.daos.{AuthTokenDAO, AuthTokenDAOService}
import models.services.{AuthTokenService, AuthTokenServiceImpl}
import net.codingwell.scalaguice.ScalaModule

class BaseModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[AuthTokenDAO].to[AuthTokenDAOService]
    bind[AuthTokenService].to[AuthTokenServiceImpl]
  }
}
