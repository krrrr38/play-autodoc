package com.krrrr38.play.autodoc

import java.io.File

import play.api.libs.json.Json
import play.api.mvc.{ Results, Action }
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatestplus.play._

class AutodocHelpersSpec extends FunSpec with Matchers with BeforeAndAfterAll with OneServerPerSuite {
  val documentPath = "doc/com/krrrr38/play/autodoc/AutodocHelpers.md"
  implicit val caller = com.krrrr38.play.autodoc.AutodocHelpers.AutodocCaller(this.getClass)

  override implicit lazy val app: FakeApplication = FakeApplication(withRoutes = {
    case ("GET", "/api/users/yuno") =>
      Action { req =>
        Results.Ok(Json.obj("user" -> Json.obj("name" -> "yuno", "height" -> 144)))
      }
    case _ => throw new IllegalStateException("invalid routes")
  })

  override protected def beforeAll(): Unit = {
    System.setProperty("play.autodoc", "1")
  }

  override protected def afterAll(): Unit = {
    new File("doc").delete
    System.setProperty("play.autodoc", "0")
  }

  describe("AutodocHelpers#autodoc") {
    it("generate document") {
      val req = FakeRequest("GET", "/api/users/yuno")
        .withHeaders("X-Secret-Key" -> "will be hide")
        .withHeaders("X-Api-Key" -> "will be converted")
        .withHeaders("X-Public-Key" -> "PUBLIC_KEY")
      val res = AutodocHelpers.autodoc(
        title = "GET /api/users/$name",
        requestHeaderConverter = {
          case ("X-Secret-Key", v) => None
          case ("X-Api-Key", v) => Some("YOUR_API_KEY")
          case (k, v) => Some(v)
        }
      ).route(req).get
      status(res) shouldBe OK

      val doc = new File(documentPath)
      doc.exists() shouldBe true
      val contents = scala.io.Source.fromFile(doc).getLines().mkString("\n")
      contents should include("## GET /api/users/$name")
      contents should not include ("X-Secret-Key")
      contents should include("X-Api-Key: YOUR_API_KEY")
      contents should include("X-Public-Key: PUBLIC_KEY")
    }
  }

}
