package controllers.api

import controllers.api.Cats.Cat
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.test.Helpers._
import play.api.test._

// it is possible to import AutodocHelpers object directly
import com.krrrr38.play.autodoc.AutodocHelpers._

@RunWith(classOf[JUnitRunner])
class CatsSpec extends Specification {

  implicit val caller = AutodocCaller(this.getClass)

  "CatsController" should {

    "return cat list" in new WithApplication {
      val res = autodoc("GET /api/cats", "get all cats")
        .route(FakeRequest(GET, "/api/cats")).get
      status(res) must equalTo(OK)
      contentType(res) must beSome.which(_ == "application/json")
      val json = contentAsJson(res)
      (json \ "cats").as[Seq[Cat]] must length(2)
    }

  }
}
