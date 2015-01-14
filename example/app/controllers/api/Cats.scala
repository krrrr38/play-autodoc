package controllers.api

import play.api.libs.json._
import play.api.mvc._

object Cats extends Controller {

  case class Cat(name: String, color: String)
  implicit val catFormat = Json.format[Cat]

  def list = Action {
    Ok(Json.obj("cats" -> Json.toJson(Seq(Cat("Tama", "white"), Cat("Boss", "brown")))))
  }

}
