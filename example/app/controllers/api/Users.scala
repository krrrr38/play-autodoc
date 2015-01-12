package controllers.api

import play.api.mvc._
import play.api.libs.json._

object Users extends Controller {

  case class User(name: String, height: Int)
  implicit val userFormat = Json.format[User]

  def list = Action {
    Ok(Json.obj("users" -> Json.toJson(Seq(User("Bob", 172), User("Alice", 155)))))
  }

  def find(name: String) = Action {
    Ok(Json.obj("user" -> Json.toJson(User(name, 163))))
  }

  def create = Action { implicit req =>
    req.body.asJson.fold(BadRequest(Json.obj("error" -> "json required"))) { json =>
      json.validate[User] match {
        case JsSuccess(user, _) => Created(Json.obj("user" -> Json.toJson(user)))
        case _ => BadRequest(Json.obj("error" -> "invalid parameters"))
      }
    }
  }
}
