package models

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import io.circe.parser.decode
import io.circe.syntax._
import org.joda.time.DateTime
import org.scalatest.FunSpec

class DecoderSpec extends FunSpec {
  describe("Circe decoding") {
    it("should decode Auth tokens") {
      val js: String =
        """
          |{
          |  "id" : "38079249-3594-4e3d-98aa-410efb6647ab",
          |  "userId" : "49658509-24d2-4d18-ae61-eb31717b24dc",
          |  "expiry" : "2019-12-28 18:36:04.255"
          |}
          |""".stripMargin

      decode[AuthToken](js) match {
        case Left(thr) => fail(thr)
        case Right(auth) => println(auth)
      }
    }
    it("should decode Users") {
      val js =
        """{
          |  "userId" : "7f16eecf-e840-4f21-8f0f-38cb1c892743",
          |  "providerId" : "credentials",
          |  "providerKey" : "fijimf@gmail.com",
          |  "firstName" : null,
          |  "lastName" : null,
          |  "fullName" : null,
          |  "email" : null,
          |  "avatarURL" : null,
          |  "activated" : false
          |}""".stripMargin
      decode[User](js) match {
        case Left(thr) => fail(thr)
        case Right(u) => println(u)
      }
    }
  }
  describe("Circe roundtrips should be idempotent") {
    it("for AuthToken") {
      val z = AuthToken(UUID.randomUUID(), UUID.randomUUID(), DateTime.now())

      decode[AuthToken](z.asJson.spaces2) match {
        case Right(z1) => assert(z === z1)
        case Left(thr) => fail(thr)
      }
    }
    it("for User") {
      val z = User(UUID.randomUUID(), LoginInfo("credentials", "fijimf@gmail.com"), Some("Jim"), Some("Frohnhofer"), None, Some("fijimf@gmail.com"), None, true)

      decode[User](z.asJson.spaces2) match {
        case Right(z1) => assert(z === z1)
        case Left(thr) => fail(thr)
      }
    }
  }
}
