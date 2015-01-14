import com.krrrr38.play.autodoc.Caller
import com.krrrr38.play.autodoc.Helpers._
import controllers.api.Users.User
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

@RunWith(classOf[JUnitRunner])
class UsersSpec extends Specification {

  implicit val caller = Caller(this.getClass)

  "UsersController" should {

    "return user list" in new WithApplication {
      val res = autodoc("GET /api/users", "get all users")
        .route(FakeRequest(GET, "/api/users")).get
      status(res) must equalTo(OK)
      contentType(res) must beSome.which(_ == "application/json")
      val json = contentAsJson(res)
      (json \ "users").as[Seq[User]] must length(2)
    }

    "return found user" in new WithApplication {
      val res = autodoc("GET /api/users/:name", "find user")
        .route(FakeRequest(GET, "/api/users/yuno")).get
      status(res) must equalTo(OK)
      contentType(res) must beSome.which(_ == "application/json")
      val json = contentAsJson(res)
      (json \ "user").as[User].name must_== "yuno"
    }

    "create user" in new WithApplication {
      val res = autodoc(
        title = "POST /api/users",
        description = "create user",
        requestHeaderConverter = {
          case (key, value) if key == "X-API-Token" => Some("YOUR_API_TOKEN")
          case (key, value) => Some(value)
        }
      ).route(
          FakeRequest(POST, "/api/users")
            .withHeaders("X-Public-Token" -> "non-secret")
            .withHeaders("X-Secret-Token" -> "which is suppressed by sbt setting")
            .withHeaders("X-API-Token" -> "which is converted by autodoc args")
            .withJsonBody(Json.obj("name" -> "yuno", "height" -> 144))
        ).get
      status(res) must equalTo(CREATED)
      contentType(res) must beSome.which(_ == "application/json")
      val json = contentAsJson(res)
      (json \ "user").as[User].name must_== "yuno"
    }

  }
}
