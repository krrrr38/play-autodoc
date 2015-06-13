import sbt._
import Keys._
import play.twirl.sbt.SbtTwirl
import play.twirl.sbt.Import.TwirlKeys

object Resolvers {
  val typesafe = "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
}

object Dependencies {
  // for autodoc-sbt
  val playPlugin = "com.typesafe.play" %% "sbt-plugin" % System.getProperty("play.version", "2.4.0")

  // for autodoc-core
  val playTest = "com.typesafe.play" %% "play-test" % System.getProperty("play.version", "2.4.0")
  val scalaTest = "org.scalatest" %% "scalatest" % "2.2.5" % "test"
  val scalaTestPlus = "org.scalatestplus" %% "play" % "1.4.0-M2" % "test"
}

object BuildSettings {

  import xerial.sbt.Sonatype.SonatypeKeys.sonatypeProfileName
  import scala.Console.{CYAN, RESET}

  val buildSettings =
    com.typesafe.sbt.SbtScalariform.scalariformSettings ++ Seq(
      organization := "com.krrrr38",
      scalaVersion := "2.10.5",
      version := "0.2.0",
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
      },
      shellPrompt := { state => s"$CYAN${name.value}$RESET > " }
    )

  val publishSettings = Seq(
    isSnapshot := false,
    sonatypeProfileName := "com.krrrr38",
    pomExtra := {
      <url>http://github.com/krrrr38/play-autodoc</url>
        <scm>
          <url>git@github.com:krrrr38/play-autodoc.git</url>
          <connection>scm:git:git@github.com:krrrr38/play-autodoc.git</connection>
        </scm>
        <licenses>
          <license>
            <name>MIT License</name>
            <url>http://www.opensource.org/licenses/mit-license.php</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
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
    pomIncludeRepository := { _ => false },
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
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
      crossScalaVersions := scalaVersion.value :: "2.11.6" :: Nil,
      resolvers += typesafe,
      libraryDependencies ++= Seq(
        playTest, scalaTest, scalaTestPlus
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
      shellPrompt := { status => "There are no contents on root project, see `projects` to change project\n> " },
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
