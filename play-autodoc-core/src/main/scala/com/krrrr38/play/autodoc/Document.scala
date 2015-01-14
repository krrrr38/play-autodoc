package com.krrrr38.play.autodoc

import java.io.{ File, FileWriter }

import akka.util.Timeout
import play.api.http.{ HeaderNames, MimeTypes, Writeable }
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, RequestHeader, Result }

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import scala.language.existentials

case class Caller(clazz: Class[_])

private[autodoc] case class Document[T](
    title: String,
    description: Option[String],
    requestHeader: RequestHeader,
    origBody: T,
    result: Result,
    requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]],
    responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]]) {
  def write(packageName: String, className: String)(implicit w: Writeable[T], timeout: Timeout): Unit = {
    val content = load(title, description, requestHeader, origBody, result, requestHeaderConverter, responseHeaderConverter)

    val outputDir =
      if (packageName.trim.isEmpty) {
        AutodocConfiguration.outputDirectory
      } else {
        AutodocConfiguration.outputDirectory + packageName.replace('.', '/') + "/"
      }
    new File(outputDir).mkdirs()
    val docFilePath = outputDir + s"${className.replaceAll("Spec$", "")}.md"

    synchronized {
      val outputFile = new File(docFilePath)
      val fw = new FileWriter(outputFile, outputFile.exists())
      fw.write(content)
      fw.close()
    }

    play.api.Logger.info(s"Autodoc writes $title in $className")
  }

  private def load(
    title: String,
    description: Option[String],
    rh: RequestHeader,
    origBody: T,
    result: Result,
    requestHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]],
    responseHeaderConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]])(implicit w: Writeable[T], timeout: Timeout): String = {
    val request = Request.parse(rh, origBody, requestHeaderConverter)
    val response = Response.parse(result, responseHeaderConverter)
    com.krrrr38.play.autodoc.templates.md.document(title, description, request, response).body
  }
}

case class Request(method: String, path: String, httpVersion: String, headers: String, body: String)
object Request {
  private[autodoc] def parse[T](rh: RequestHeader, origBody: T, headerConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]])(implicit w: Writeable[T]): Request = {
    val headers = normalizeText(
      (for {
        kvs <- rh.headers.toMap
        headerKey = kvs._1
        headerValue <- kvs._2
        converted <- AutodocConfiguration.convertRequestHeader((headerKey, headerValue), headerConverter)
      } yield s"${converted._1}: ${converted._2}").mkString("\n"), "\n")
    val body = normalizeText(origBody match {
      case AnyContentAsJson(json) => Json.prettyPrint(json)
      case content => new String(w.transform(content))
    }, "\n\n")
    Request(rh.method, rh.path, rh.version, headers, body)
  }
}

case class Response(status: String, headers: String, body: String)
object Response {
  private[autodoc] def parse(result: Result, headerConverter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]])(implicit timeout: Timeout): Response = {
    val headers = normalizeText(
      result.header.headers
        .flatMap(kv => AutodocConfiguration.convertResponseHeader(kv, headerConverter))
        .map(kv => s"${kv._1}: ${kv._2}")
        .mkString("\n"), "\n")
    val responseBodyGen: Iteratee[Array[Byte], StringBuilder] =
      Iteratee.fold(new StringBuilder) { (builder, bytes) =>
        builder.append(new String(bytes))
      }
    val responseBodyText =
      Await.result(result.body.run(responseBodyGen).map { _.toString() }, timeout.duration)
    val body = normalizeText(result.header.headers.get(HeaderNames.CONTENT_TYPE) match {
      case Some(contentType) if contentType.startsWith(MimeTypes.JSON) =>
        Try(Json.parse(responseBodyText)).map(Json.prettyPrint) getOrElse responseBodyText
      case _ => responseBodyText
    }, "\n\n")
    Response(result.header.status.toString, headers, body)
  }
}

