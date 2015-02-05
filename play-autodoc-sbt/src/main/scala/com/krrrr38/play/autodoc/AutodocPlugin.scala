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
    val autodocTocForGitHub = settingKey[Boolean]("Add toc menu in document top")
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
    val autodocTocForGitHub: Boolean = false
  }

  lazy val Autodoc = config("autodoc") extend Test

  import AutodocKeys._

  override def trigger: PluginTrigger = allRequirements

  override def requires: Plugins = play.PlayScala

  override def projectConfigurations: Seq[Configuration] = Seq(Autodoc)

  override def projectSettings: Seq[Def.Setting[_]] = autodocSettings ++ Seq(
    resolvers += "Maven Repository on Github" at "http://krrrr38.github.io/maven/",
    libraryDependencies += "com.krrrr38" %% "play-autodoc-core" % (autodocVersion in Autodoc).value % "test"
  )

  case class AutodocSetting(
    outputDir: String = AutodocDefaults.autodocOutputDirectory,
    cacheDir: String = AutodocDefaults.autodocCacheDirectory,
    enableGithubLikeTocMenu: Boolean = AutodocDefaults.autodocTocForGitHub)
  var setting = AutodocSetting()

  val confFile = "autodoc.conf"
  lazy val autodocSettings = inConfig(Autodoc)(Defaults.testTasks ++
    Seq(
      autodocConfGen := Tasks.confGen(
        streams.value.log,
        resourceDirectory.value,
        autodocOutputDirectory.value,
        autodocCacheDirectory.value,
        autodocSuppressedRequestHeaders.value,
        autodocSuppressedResponseHeaders.value
      ),
      autodocSaveSetting := { setting = AutodocSetting(autodocOutputDirectory.value, autodocCacheDirectory.value, autodocTocForGitHub.value) },
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
      autodocTocForGitHub := (autodocTocForGitHub ?? AutodocDefaults.autodocTocForGitHub).value,
      test <<= (test in Autodoc).result.map { result => // XXX is it possible to get outputdir and cachedir setting values in here?
      Tasks.shallowMove(file(setting.cacheDir), file(setting.outputDir), setting.enableGithubLikeTocMenu)
    } dependsOn (autodocClean, autodocConfGen, autodocSaveSetting),
    testOnly <<= (testOnly in Autodoc).mapR { result => // XXX InputKey cannot call `result`...
      Tasks.shallowMove(file(setting.cacheDir), file(setting.outputDir), setting.enableGithubLikeTocMenu)
    } dependsOn (autodocCleanCache, autodocConfGen, autodocSaveSetting),
    testQuick <<= (testQuick in Autodoc).mapR { result => // XXX InputKey cannot call `result`...
      Tasks.shallowMove(file(setting.cacheDir), file(setting.outputDir), setting.enableGithubLikeTocMenu)
    } dependsOn (autodocCleanCache, autodocConfGen, autodocSaveSetting),
    javaOptions ++= Seq("-Dplay.autodoc=true")
  ))

  // To prevent autodoc setting in PlayPlugin enabled project, add this setting to it
  lazy val autodocOffSettings = inConfig(Autodoc)(Seq(
    test := (test in Test),
    testOnly := (testOnly in Test),
    testQuick := (testQuick in Test),
    javaOptions += "-Dplay.autodoc=false"
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
    def shallowMove(fromDir: File, toDir: File, withToc: Boolean): Unit = {
      def shallowMoveFile(fromFile: File, toFile: File): Unit = {
        if (toFile.exists())
          IO.delete(toFile)
        if (withToc)
          IO.write(toFile, GitHubMarkdown.addToc(fromFile))
        else
          moveFile(fromFile, toFile)
      }
      def shallowMoveR(fromFileOrDir: File, toFileOrDir: File): Unit = (fromFileOrDir.isDirectory, toFileOrDir.exists(), toFileOrDir.isDirectory) match {
        case (false, _, _) =>
          shallowMoveFile(fromFileOrDir, toFileOrDir)
        case (true, false, _) =>
          toFileOrDir.mkdirs()
          fromFileOrDir.listFiles.foreach( _fromFile => shallowMoveR(_fromFile, toFileOrDir / _fromFile.name))
        case (true, true, false) =>
          toFileOrDir.delete()
          toFileOrDir.mkdirs()
          fromFileOrDir.listFiles.foreach( _fromFile => shallowMoveR(_fromFile, toFileOrDir / _fromFile.name))
        case (true, true, true) =>
          fromFileOrDir.listFiles.foreach( _fromFile => shallowMoveR(_fromFile, toFileOrDir / _fromFile.name))
      }

      if (fromDir.exists())
        shallowMoveR(fromDir, toDir)
    }

    object GitHubMarkdown {
      def addToc(file: File): String = {
        val contents = IO.read(file)
        val toc = genrateToc(contents)
        toc + contents
      }

      def genrateToc(contents: String): String = {
        val idCounter = scala.collection.mutable.Map.empty[String, Int]
        // covert space to `-`, delete other symbols except `-` and `_`
        // if same id is existed, add `-index` to last
        def convertToGithubHashName(line: String): String = {
          val base = line.replaceAll("\\s", "-").replaceAll("""[!"#\$%&'\(\)\*\+,\.\/:;<=>\?@\[\\\]^`{\|}~]""", "")
          val maybeIndex: Option[Int] = {
            val maybeIndex = idCounter.get(base)
            maybeIndex match {
              case Some(index) => idCounter += (base -> (index+1))
              case None => idCounter += (base -> 1)
            }
            maybeIndex
          }
          maybeIndex.map(index => s"user-content-$base-$index").getOrElse(s"user-content-$base")
        }
        val toc = contents.split("\n")
          .filter(_.startsWith("## "))
          .map { titleLine =>
          val title = titleLine.dropWhile(_ == '#').trim
          val id = convertToGithubHashName(title.toLowerCase)
          s"  - [$title](#$id)"
        }.mkString("\n")
        s"## API Documentation\n- Table of Contents\n$toc\n"
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
