
import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}


package object models {
  val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
  implicit val dateTimeDecoder: Decoder[DateTime] = Decoder.decodeString.map { s => DateTime.parse(s, dateFormatter) }
  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap { d => d.toString(dateFormatter) }
  implicit val authTokenEncoder: Encoder.AsObject[AuthToken] = deriveEncoder[AuthToken]
  implicit val authTokenDecoder: Decoder[AuthToken] = deriveDecoder[AuthToken]
  implicit val loginInfoEncoder: Encoder.AsObject[LoginInfo] = deriveEncoder[LoginInfo]
  implicit val loginInfoDecoder: Decoder[LoginInfo] = deriveDecoder[LoginInfo]
  implicit val passwordInfoEncoder: Encoder.AsObject[PasswordInfo] = deriveEncoder[PasswordInfo]
  implicit val passwordInfoDecoder: Decoder[PasswordInfo] = deriveDecoder[PasswordInfo]

  implicit val encodeFoo: Encoder[User] = new Encoder[User] {
    final def apply(u: User): Json = Json.obj(
      ("userId", Json.fromString(u.userId.toString)),
      ("providerId", Json.fromString(u.loginInfo.providerID)),
      ("providerKey", Json.fromString(u.loginInfo.providerKey)),
      ("firstName", u.firstName.asJson),
      ("lastName", u.lastName.asJson),
      ("fullName", u.fullName.asJson),
      ("email", u.email.asJson),
      ("avatarURL", u.avatarURL.asJson),
      ("activated", Json.fromBoolean(u.activated))
    )
  }

  implicit val decodeFoo: Decoder[User] = new Decoder[User] {
    final def apply(c: HCursor): Decoder.Result[User] =
      for {
        userId <- c.downField("userId").as[String]
        providerId <- c.downField("providerId").as[String]
        providerKey <- c.downField("providerKey").as[String]
        firstName <- c.downField("firstName").as[Option[String]]
        lastName <- c.downField("lastName").as[Option[String]]
        fullName <- c.downField("fullName").as[Option[String]]
        email <- c.downField("email").as[Option[String]]
        avatarURL <- c.downField("avatarURL").as[Option[String]]
        activated <- c.downField("activated").as[Boolean]
      } yield {
        User(UUID.fromString(userId), LoginInfo(providerId, providerKey), firstName, lastName, fullName, email, avatarURL, activated)
      }
  }
}
