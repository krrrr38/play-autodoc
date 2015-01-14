package com.krrrr38.play.autodoc

import sbt.Keys._
import sbt._

object AutodocPlugin extends sbt.AutoPlugin {
  object AutodocKeys {
    val autodocVersion = settingKey[String]("autodoc version")
    val autodocOutputDirectory = settingKey[String]("""Autodoc generated document directory. Default `"doc"`""")
    val autodocCacheDirectory = taskKey[String]("Before generating documents, output them into cache directory, then merge them") // XXX to be settingKey?
    val autodocSuppressedRequestHeaders = settingKey[Seq[String]]("Prevent to show request header in autodoc document")
    val autodocSuppressedResponseHeaders = settingKey[Seq[String]]("Prevent to show response header in autodoc document")
    val autodocConfGen = taskKey[File]("Generate autodoc configuration.")
    val autodocSaveSetting = taskKey[Unit]("Save setting in autodoc")
    val autodocCleanCache = taskKey[Unit]("delete autodoc cache directory")
    val autodocClean = taskKey[Unit]("delete autodoc document directory and conf file")
  }

  object AutodocDefaults {
    val autodocOutputDirectory: String = "doc"
    val autodocCacheDirectory: String = "target/autodoc-cache"
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

  case class AutodocSetting(outputDir: String = AutodocDefaults.autodocOutputDirectory, cacheDir: String = AutodocDefaults.autodocCacheDirectory)
  var setting = AutodocSetting()

  val confFile = "autodoc.conf"
  lazy val autodocSettings = inConfig(Autodoc)(Seq(
    autodocConfGen := Tasks.confGen(
      streams.value.log,
      resourceDirectory.value,
      autodocOutputDirectory.value,
      autodocCacheDirectory.value,
      autodocSuppressedRequestHeaders.value,
      autodocSuppressedResponseHeaders.value
    ),
    autodocSaveSetting := { setting = AutodocSetting(autodocOutputDirectory.value, autodocCacheDirectory.value) },
    autodocCleanCache := Tasks.deleteFile(baseDirectory.value, autodocCacheDirectory.value),
    autodocClean := {
      Tasks.deleteFile(baseDirectory.value, autodocOutputDirectory.value)
      Tasks.deleteFile(resourceDirectory.value, confFile)
    },
    autodocVersion := AutodocVersion.value,
    autodocOutputDirectory := (autodocOutputDirectory ?? AutodocDefaults.autodocOutputDirectory).value,
    autodocCacheDirectory := IO.relativize(baseDirectory.value, target(_ / "autodoc-cache").value).getOrElse(AutodocDefaults.autodocCacheDirectory),
    autodocSuppressedRequestHeaders := (autodocSuppressedRequestHeaders ?? AutodocDefaults.autodocSuppressedRequestHeaders).value,
    autodocSuppressedResponseHeaders := (autodocSuppressedResponseHeaders ?? AutodocDefaults.autodocSuppressedResponseHeaders).value,
    test <<= test.result.map { result => // XXX is it possible to get outputdir and cachedir setting values in here?
      Tasks.moveFile(file(setting.cacheDir), file(setting.outputDir))
    } dependsOn (autodocClean, autodocConfGen, autodocSaveSetting),
    testOnly <<= testOnly.mapR { result => // XXX InputKey cannot call `result`...
      Tasks.shallowMove(file(setting.cacheDir), file(setting.outputDir))
    } dependsOn (autodocCleanCache, autodocConfGen, autodocSaveSetting),
    testQuick <<= testQuick.mapR { result => // XXX InputKey cannot call `result`...
      Tasks.shallowMove(file(setting.cacheDir), file(setting.outputDir))
    } dependsOn (autodocCleanCache, autodocConfGen, autodocSaveSetting),
    (javaOptions in Test) += "-Dplay.autodoc=true"
  ))

  object Tasks {
    def confGen(logger: Logger, resourceDir: File, outputDir: String, cacheDir: String, supReqHeaders: Seq[String], supResHeaders: Seq[String]): File = {
      val conf = resourceDir / confFile
      val content = Configuration.generate(outputDir, cacheDir, supReqHeaders, supResHeaders)
      if (!conf.exists() || IO.read(conf) != content){
        IO.write(conf, content)
        logger.info(s"Generate $confFile in test resource direcotry.")
      }
      conf
    }

    def moveFile(fromFile: File, toFile: File) {
      IO.delete(toFile)
      Option(toFile.getParentFile).foreach(IO.createDirectory) // sbt.IO.move doesnot support files without parent dir.
      fromFile.renameTo(toFile)
    }

    def deleteFile(base: File, filename: String): Unit = IO.delete(base / filename)

    // compare fromDir and toDir, move fromDir contents to toDir.
    // if no equivalent toDir contents in fromDir, they would be kept.
    def shallowMove(fromDir: File, toDir: File): Unit = {
      def shallowMoveR(fromFile: File, toFile: File): Unit = fromFile.listFiles().foreach { _fromFile =>
        val _toFile = toFile / _fromFile.name
        (_toFile.exists, _toFile.isDirectory, _fromFile.isDirectory) match {
          case (false, _,     _)     => moveFile(_fromFile, _toFile)
          case (true,  false, _)     => moveFile(_fromFile, _toFile)
          case (true,  true,  false) => moveFile(_fromFile, _toFile)
          case (true,  true,  true)  => shallowMoveR(_fromFile, _toFile)
        }
      }
      if (fromDir.exists && fromDir.isDirectory) {
        if (toDir.exists) {
          shallowMoveR(fromDir, toDir)
          IO.delete(fromDir)
        } else {
          moveFile(fromDir, toDir)
        }
      }
    }
  }

  object Configuration {
    // XXX same as com.krrrr38.play.autodoc.AutodocConfiguration.Keys
    private val AUTODOC_CONFIG_KEY = "autodoc."
    object Keys {
      val OUTPUT_DIRECTORY = AUTODOC_CONFIG_KEY + "outputDirectory"
      val CACHE_DIRECTORY = AUTODOC_CONFIG_KEY + "cacheDirectory"
      val SUPPRESSED_REQUEST_HEADERS = AUTODOC_CONFIG_KEY + "suppressedRequestHeaders"
      val SUPPRESSED_RESPONSE_HEEADERS = AUTODOC_CONFIG_KEY + "suppressedResponseHeaders"
    }

    def generate(outputDir: String, cacheDir: String, suppressedRequestHeaders: Seq[String], suppressedResponseHeaders: Seq[String]): String = {
      val suppressedRequestHeadersStr = suppressedRequestHeaders.map(header => s""""$header"""").mkString(",")
      val suppressedResponseHeadersStr = suppressedResponseHeaders.map(header => s""""$header"""").mkString(",")
      s"""
         |${Keys.OUTPUT_DIRECTORY}=%s
         |${Keys.CACHE_DIRECTORY}=%s
         |${Keys.SUPPRESSED_REQUEST_HEADERS}=%s
         |${Keys.SUPPRESSED_RESPONSE_HEEADERS}=%s
        """.stripMargin.format(
          s""""$outputDir"""",
          s""""$cacheDir"""",
          s"""[$suppressedRequestHeadersStr]""",
          s"""[$suppressedResponseHeadersStr]"""
        )
    }
  }
}
