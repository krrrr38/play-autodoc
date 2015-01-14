package com.krrrr38.play.autodoc

import java.io.File

import com.typesafe.config.ConfigFactory
import play.api.{ Configuration, Logger }

import scala.util.{ Properties, Try }

object AutodocConfiguration {
  val CONFIG_FILE_NAME = "/autodoc.conf"
  val AUTODOC_CONFIG_KEY = "autodoc."
  object Keys {
    val OUTPUT_DIRECTORY = AUTODOC_CONFIG_KEY + "outputDirectory"
    val CACHE_DIRECTORY = AUTODOC_CONFIG_KEY + "cacheDirectory"
    val SUPPRESSED_REQUEST_HEADERS = AUTODOC_CONFIG_KEY + "suppressedRequestHeaders"
    val SUPPRESSED_RESPONSE_HEADERS = AUTODOC_CONFIG_KEY + "suppressedResponseHeaders"
  }
  object Values {
    val DEFAULT_OUTPUT_DIRECTORY: String = "doc"
    val DEFAULT_SUPPRESSED_REQUEST_HEADERS: Seq[String] = Nil
    val DEFAULT_SUPPRESSED_RESPONSE_HEADERS: Seq[String] = Nil
  }

  lazy val config: Configuration = {
    (for {
      resource <- Option(getClass.getResource(CONFIG_FILE_NAME))
      file = new File(resource.getFile)
      if file.canRead
      conf <- Try(Configuration(ConfigFactory.parseFile(file))).toOption
    } yield conf).getOrElse {
      Logger.error(s"""
            |Failed to load $CONFIG_FILE_NAME.
            |
            |play-autodoc-sbt plugin generate it automatically.
            |Please see https://github.com/krrrr38/play-autodoc/blob/master/README.md
          """.stripMargin)
      Configuration.empty
    }
  }

  // if cache directory is set by sbt plugin, the plugin will try to merge them to output dir, so output into cache dir
  lazy val outputDirectory = {
    val outputDir = config.getString(Keys.CACHE_DIRECTORY).getOrElse {
      config.getString(Keys.OUTPUT_DIRECTORY).getOrElse(Values.DEFAULT_OUTPUT_DIRECTORY)
    }
    if (outputDir.endsWith("/")) outputDir else outputDir + "/"
  }
  lazy val suppressedRequestHeaders = config.getStringSeq(Keys.SUPPRESSED_REQUEST_HEADERS).getOrElse(Values.DEFAULT_SUPPRESSED_REQUEST_HEADERS)
  lazy val suppressedResponseHeaders = config.getStringSeq(Keys.SUPPRESSED_RESPONSE_HEADERS).getOrElse(Values.DEFAULT_SUPPRESSED_RESPONSE_HEADERS)

  private[autodoc] def convertRequestHeader(header: (HeaderKey, HeaderValue), converter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]]): Option[(HeaderKey, HeaderValue)] =
    convertHeader(header, suppressedRequestHeaders, converter)
  private[autodoc] def convertResponseHeader(header: (HeaderKey, HeaderValue), converter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]]): Option[(HeaderKey, HeaderValue)] =
    convertHeader(header, suppressedResponseHeaders, converter)

  private def convertHeader(header: (HeaderKey, HeaderValue), defaultSuppressedHeaderKeys: Seq[String], converter: PartialFunction[(HeaderKey, HeaderValue), Option[HeaderValue]]) =
    if (defaultSuppressedHeaderKeys.contains(header._1)) {
      None
    } else {
      converter(header).map(value => (header._1, value))
    }

  def enable: Boolean = isTrue(Properties.propOrNone("play.autodoc").getOrElse(""))

  private def isTrue(str: String): Boolean =
    str.trim.nonEmpty && str != "0" && str != "false"
}
