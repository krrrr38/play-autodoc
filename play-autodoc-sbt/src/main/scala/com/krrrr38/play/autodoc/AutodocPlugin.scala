package com.krrrr38.play.autodoc

import sbt.Keys._
import sbt._

object AutodocPlugin extends sbt.AutoPlugin {
  object AutodocKeys {
    val autodocVersion = settingKey[String]("autodoc version")
    val autodocOutputDirectory = settingKey[String]("""Autodoc generated document directory. Default `"doc"`""")
    val autodocSuppressedRequestHeaders = settingKey[Seq[String]]("Prevent to show request header in autodoc document")
    val autodocSuppressedResponseHeaders = settingKey[Seq[String]]("Prevent to show response header in autodoc document")
    val autodocConfGen = taskKey[File]("Generate autodoc configuration.")
    val autodocCleanup = taskKey[Unit]("delete autodoc documents.")
  }

  object AutodocDefaults {
    val autodocOutputDirectory: String = "doc"
    val autodocSuppressedRequestHeaders: Seq[String] = Nil
    val autodocSuppressedResponseHeaders: Seq[String] = Nil
  }

  lazy val Autodoc = config("autodoc") extend Test

  import AutodocKeys._

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = play.PlayScala

  override def projectConfigurations: Seq[Configuration] = Seq(Autodoc)

  override def projectSettings: Seq[Def.Setting[_]] = autodocSettings ++ Seq(
    libraryDependencies += "com.krrrr38" %% "play-autodoc-core" % (autodocVersion in Autodoc).value % "test"
  )

  val PROPERTY_KEY = "play.autodoc"
  lazy val autodocSettings = inConfig(Autodoc)(Seq(
    autodocConfGen := Tasks.confGen(
      streams.value.log,
      resourceDirectory.value,
      autodocOutputDirectory.value,
      autodocSuppressedRequestHeaders.value,
      autodocSuppressedResponseHeaders.value
    ),
    autodocCleanup := Tasks.cleanup(baseDirectory.value, autodocOutputDirectory.value),
    autodocVersion := AutodocVersion.value,
    autodocOutputDirectory := (autodocOutputDirectory ?? AutodocDefaults.autodocOutputDirectory).value,
    autodocSuppressedRequestHeaders := (autodocSuppressedRequestHeaders ?? AutodocDefaults.autodocSuppressedRequestHeaders).value,
    autodocSuppressedResponseHeaders := (autodocSuppressedResponseHeaders ?? AutodocDefaults.autodocSuppressedResponseHeaders).value,
    (compile in Test) <<= (compile in Test) dependsOn autodocConfGen,
    test <<= test dependsOn autodocCleanup,
    testOnly <<= testOnly dependsOn autodocCleanup, // XXX when run test only, delete only target doc file
    (javaOptions in Test) += "-Dplay.autodoc=true"
  ))

  object Tasks {
    def confGen(logger: Logger, resourceDir: File, outputFile: String, supReqHeaders: Seq[String], supResHeaders: Seq[String]): File = {
      val conf = resourceDir / "autodoc.conf"
      val content = Configuration.generate(outputFile, supReqHeaders, supResHeaders)
      if (!conf.exists() || IO.read(conf) != content){
        IO.write(conf, content)
        logger.info("Generate `autodoc.conf` in test resource direcotry.")
      }
      conf
    }

    def cleanup(base: File, outputDir: String): Unit = IO.delete(base / outputDir)
  }

  object Configuration {
    // XXX same as com.krrrr38.play.autodoc.AutodocConfiguration.Keys
    private val AUTODOC_CONFIG_KEY = "autodoc."
    object Keys {
      val OUTPUT_DIRECTORY = AUTODOC_CONFIG_KEY + "outputDirectory"
      val SUPPRESSED_REQUEST_HEADERS = AUTODOC_CONFIG_KEY + "suppressedRequestHeaders"
      val SUPPRESSED_RESPONSE_HEEADERS = AUTODOC_CONFIG_KEY + "suppressedResponseHeaders"
    }

    def generate(outputFile: String, suppressedRequestHeaders: Seq[String], suppressedResponseHeaders: Seq[String]): String = {
      val suppressedRequestHeadersStr = suppressedRequestHeaders.map(header => s""""$header"""").mkString(",")
      val suppressedResponseHeadersStr = suppressedResponseHeaders.map(header => s""""$header"""").mkString(",")
      s"""
         |${Keys.OUTPUT_DIRECTORY}="$outputFile"
         |${Keys.SUPPRESSED_REQUEST_HEADERS}=%s
         |${Keys.SUPPRESSED_RESPONSE_HEEADERS}=%s
        """.stripMargin.format(
          s"""[$suppressedRequestHeadersStr]""",
          s"""[$suppressedResponseHeadersStr]"""
        )
    }
  }
}
