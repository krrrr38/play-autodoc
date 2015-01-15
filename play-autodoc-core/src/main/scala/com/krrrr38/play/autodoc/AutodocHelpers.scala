package com.krrrr38.play.autodoc

import play.api.Application
import play.api.http.Writeable
import play.api.mvc._
import play.api.test._

import scala.concurrent.Future

/**
 * autodoc helper to use `route` in RouteInvokers
 */
object AutodocHelpers extends AutodocHelpers

/**
 * autodoc helper to use `route` in RouteInvokers
 */
trait AutodocHelpers {
  /**
   * Annotate autodoc test to generate documents
   * @param title endpoint document title
   * @param description endpoint document description
   * @param requestHeaderConverter condition for suppressing or converting header keys and values. if return None, not output target header.
   * @param responseHeaderConverter condition for suppressing or converting header keys and values. if return None, not output target header.
   * @param caller caller class to detect which controller treats this endpoint
   * @return
   */
  def autodoc(
    title: String = "",
    description: String = "",
    requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]] = IDENTITY_HEADER_CONVERTER,
    responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]] = IDENTITY_HEADER_CONVERTER)(implicit caller: Caller): RecordableRouteInvoker = {
    val maybeTitle = if (title.trim.nonEmpty) Some(title) else None
    val maybeDescription = if (description.trim.nonEmpty) Some(description) else None
    new RecordableRouteInvoker(caller, maybeTitle, maybeDescription, requestHeaderConverter, responseHeaderConverter)
  }

  /**
   * output identity header value converter
   */
  private val IDENTITY_HEADER_CONVERTER: PartialFunction[(String, String), Option[String]] = {
    case (key, value) => Some(value)
  }

  private[autodoc] class RecordableRouteInvoker(
      caller: Caller,
      maybeTitle: Option[String],
      maybeDescription: Option[String],
      requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]],
      responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]]) extends Writeables with RouteInvokers with DefaultAwaitTimeout {
    import scala.concurrent.ExecutionContext.Implicits.global
    override def route[T](app: Application, rh: RequestHeader, body: T)(implicit w: Writeable[T]): Option[Future[Result]] = {
      val maybeResult = super.route(app, rh, body)
      if (AutodocConfiguration.enable) {
        val packageName = Option(caller.clazz.getPackage).map(_.getName).getOrElse("")
        val className = caller.clazz.getSimpleName
        maybeResult.foreach(_.map { result =>
          val title = maybeTitle.getOrElse(s"${rh.method} ${rh.path}")
          Document(title, maybeDescription, rh, body, result, requestHeaderConverter, responseHeaderConverter).write(packageName, className)
          result
        })
      }
      maybeResult
    }
  }
}
