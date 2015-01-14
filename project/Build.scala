import sbt._
import Keys._
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

object Resolvers {
  val typesafe = "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
}

object Dependencies {
  // for autodoc-sbt
  val playPlugin = "com.typesafe.play" % "sbt-plugin" % System.getProperty("play.version", "2.3.4")

  // for autodoc-core
  val playTest = "com.typesafe.play" %% "play-test" % System.getProperty("play.version", "2.3.4")
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"
}

object BuildSettings {
  import scala.Console.{ CYAN, RESET }

  val buildSettings =
    com.typesafe.sbt.SbtScalariform.scalariformSettings ++ Seq(
      organization := "com.krrrr38",
      scalaVersion := "2.10.4",
      version := "0.0.1",
      scalacOptions ++= (
        "-deprecation" ::
          "-feature" ::
          "-unchecked" ::
          "-Xlint" ::
          Nil
        ),
      scalacOptions ++= {
        if (scalaVersion.value.startsWith("2.11"))
          Seq("-Ywarn-unused", "-Ywarn-unused-import")
        else
          Nil
      }
    )

  val publishSettings = Seq(
    isSnapshot := true,
    pomExtra := {
      <url>http://github.com/krrrr38/mackerel-client-scala</url>
        <scm>
          <url>git@github.com:krrrr38/mackerel-client-scala.git</url>
          <connection>scm:git:git@github.com:krrrr38/mackerel-client-scala.git</connection>
        </scm>
        <developers>
          <developer>
            <id>krrrr38</id>
            <name>Ken Kaizu</name>
            <url>http://www.krrrr38.com</url>
          </developer>
        </developers>
    },
    publishArtifact in Test := false,
    publishMavenStyle := true,
    shellPrompt := { state => s"$CYAN${name.value}$RESET > " },
    publishTo := {
      val ghpageMavenDir: Option[String] =
        if((Process("which ghq") #>> new java.io.File("/dev/null")).! == 0) {
          (Process("ghq list --full-path") #| Process("grep krrrr38/maven")).lines.headOption
        } else None
      ghpageMavenDir.map { dirPath =>
        Resolver.file(
          organization.value,
          file(dirPath)
        )(Patterns(true, Resolver.mavenStyleBasePattern))
      }
    }
  )
}

object PlayAutodocBuild extends Build {
  import BuildSettings._
  import Resolvers._
  import Dependencies._

  lazy val autodoc = Project(
    "play-autodoc-core",
    file("play-autodoc-core"),
    settings = buildSettings ++ publishSettings ++ Seq(
      name := "play-autodoc-core",
      description := "Generate documentation from your play application request tests.",
      crossScalaVersions := scalaVersion.value :: "2.11.4" :: Nil,
      resolvers += typesafe,
      libraryDependencies ++= Seq(
        playTest, scalaTest
      ),
      TwirlKeys.templateImports ++= Seq(
        "com.krrrr38.play.autodoc.{ Request, Response }"
      ),
      TwirlKeys.templateFormats += ("md" -> "com.krrrr38.play.autodoc.twirl.MarkdownFormat")
    )
  ).enablePlugins(SbtTwirl)

  lazy val autodocPlugin = Project(
    "play-autodoc-sbt",
    file("play-autodoc-sbt"),
    settings = buildSettings ++ publishSettings ++ Seq(
      name := "play-autodoc-sbt",
      description := "Generate documentation from your play application request tests.",
      sbtPlugin := true,
      resolvers += typesafe,
      addSbtPlugin(playPlugin),
      sourceGenerators in Compile <+= (version, sourceManaged in Compile) map Tasks.AutodocVersion
    )
  )

  lazy val root = Project(
    "root",
    file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      shellPrompt := { status => "There are no contents on root project, see `projects` to change project\n> "},
      packagedArtifacts := Map.empty // prevent publishing
    )
  ).aggregate(autodoc, autodocPlugin)
}

object Tasks {
  def AutodocVersion(version: String, dir: File): Seq[File] = {
    val file = dir / "AutodocVersion.scala"
    IO.write(file,
      """package com.krrrr38.play.autodoc
        |
        |object AutodocVersion {
        |  val value = "%s"
        |}
      """.stripMargin.format(version))
    Seq(file)
  }
}