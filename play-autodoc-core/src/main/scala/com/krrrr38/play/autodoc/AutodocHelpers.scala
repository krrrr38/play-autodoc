package com.krrrr38.play.autodoc

import play.api.Application
import play.api.http.Writeable
import play.api.mvc._
import play.api.test._

import scala.concurrent.Future

import scala.language.existentials

/**
 * autodoc helper to use `route` in RouteInvokers
 */
object AutodocHelpers extends AutodocHelpers

/**
 * autodoc helper to use `route` in RouteInvokers
 */
trait AutodocHelpers {
  /**
   * request caller class wrapper
   * @param clazz
   */
  case class AutodocCaller(clazz: Class[_])

  /**
   * Annotate autodoc test to generate documents
   * @param title endpoint document title
   * @param description endpoint document description
   * @param requestHeaderConverter condition for suppressing or converting header keys and values. if return None, not output target header.
   * @param responseHeaderConverter condition for suppressing or converting header keys and values. if return None, not output target header.
   * @param responseBodyParser convert response result into String.
   * @param caller caller class to detect which controller treats this endpoint
   * @return
   */
  def autodoc(
    title: String = "",
    description: String = "",
    requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]] = IDENTITY_HEADER_CONVERTER,
    responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]] = IDENTITY_HEADER_CONVERTER,
    responseBodyParser: (Result) => String = Response.DEFAULT_BODY_PARSER)(implicit caller: AutodocCaller): RecordableRouteInvoker = {
    val maybeTitle = if (title.trim.nonEmpty) Some(title) else None
    val maybeDescription = if (description.trim.nonEmpty) Some(description) else None
    new RecordableRouteInvoker(caller, maybeTitle, maybeDescription, requestHeaderConverter, responseHeaderConverter, responseBodyParser)
  }

  /**
   * output identity header value converter
   */
  private val IDENTITY_HEADER_CONVERTER: PartialFunction[(String, String), Option[String]] = {
    case (key, value) => Some(value)
  }

  private[autodoc] class RecordableRouteInvoker(
      caller: AutodocCaller,
      maybeTitle: Option[String],
      maybeDescription: Option[String],
      requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]],
      responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]],
      responseBodyParser: (Result) => String) extends Writeables with RouteInvokers with DefaultAwaitTimeout {
    import scala.concurrent.ExecutionContext.Implicits.global
    override def route[T](app: Application, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Future[Result]] = {
      val maybeResult = super.route(app, rh, body)
      if (AutodocConfiguration.enable) {
        val packageName = Option(caller.clazz.getPackage).map(_.getName).getOrElse("")
        val className = caller.clazz.getSimpleName
        maybeResult.foreach(_.map { result =>
          val title = maybeTitle.getOrElse {
            if (rh.rawQueryString.isEmpty) {
              s"${rh.method} ${rh.path}"
            } else {
              s"${rh.method} ${rh.path}?${rh.rawQueryString}"
            }
          }
          Document(title, maybeDescription, rh, body, result, requestHeaderConverter, responseHeaderConverter, responseBodyParser).write(packageName, className)
          result
        })
      }
      maybeResult
    }
  }
}
