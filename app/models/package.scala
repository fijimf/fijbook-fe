
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
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
  implicit val userEncoder: Encoder.AsObject[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val passwordInfoEncoder: Encoder.AsObject[PasswordInfo] = deriveEncoder[PasswordInfo]
  implicit val passwordInfoDecoder: Decoder[PasswordInfo] = deriveDecoder[PasswordInfo]
}
