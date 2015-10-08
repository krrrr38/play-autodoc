package com.krrrr38.play.autodoc

import java.io.File

import org.scalatest.{ BeforeAndAfterAll, FunSpec, Matchers }

import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.FakeApplication

import play.api.test._
import play.api.test.Helpers._

class AutodocHelpersSpec extends FunSpec with Matchers with BeforeAndAfterAll {
  val documentPath = "doc/com/krrrr38/play/autodoc/AutodocHelpers.md"
  implicit val caller = com.krrrr38.play.autodoc.AutodocHelpers.AutodocCaller(this.getClass)

  override protected def beforeAll(): Unit = {
    System.setProperty("play.autodoc", "1")
  }

  override protected def afterAll(): Unit = {
    delete(new File("doc"))
    System.setProperty("play.autodoc", "0")
  }

  def delete(file: File) {
    if (file.isDirectory) Option(file.listFiles).map(_.toList).getOrElse(Nil).foreach(delete(_))
    file.delete
  }

  def fakeApp = FakeApplication(withRoutes = {
    case ("GET", "/api/users/yuno") =>
      Action { req =>
        Results.Ok(Json.obj("user" -> Json.obj("name" -> "yuno", "height" -> 144)))
      }
    case _ => throw new IllegalStateException("invalid routes")
  })

  describe("AutodocHelpers#autodoc") {
    it("generate document") {
      new WithServer(fakeApp) {
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
        Thread.sleep(100)

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

}
