package models

import org.scalatest.FunSpec
import io.circe.parser.decode

class DecoderSpec extends FunSpec {
describe("Circe decoding"){
  it ("should decode Auth tokens"){
    val js: String =
      """
        |{
        |  "id" : "38079249-3594-4e3d-98aa-410efb6647ab",
        |  "userId" : "49658509-24d2-4d18-ae61-eb31717b24dc",
        |  "expiry" : "2019-12-28 18:36:04.255"
        |}
        |""".stripMargin

    decode[AuthToken](js) match {
      case Left(thr)=>fail(thr)
      case Right(auth)=>println(auth)
    }
  }
}
}
