import sbt._
import Keys._

import com.krrrr38.play.autodoc.AutodocPlugin.AutodocKeys

object PlayScalaBuild extends Build {
  import play.sbt.PlayImport.specs2
  val dependencies = Seq(
    specs2 % Test
  )

  lazy val root = Project(
    "play-scala",
    file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      scalaVersion := "2.10.5",
      crossScalaVersions := scalaVersion.value :: "2.11.6" :: Nil,
      scalacOptions ++= (
        "-deprecation" ::
          "-feature" ::
          "-unchecked" ::
          "-Xlint" ::
          Nil
        ),
      libraryDependencies ++= dependencies,
      AutodocKeys.autodocOutputDirectory := "doc",
      AutodocKeys.autodocSuppressedRequestHeaders := Seq("X-Secret-Token"),
      AutodocKeys.autodocSuppressedResponseHeaders := Nil,
      AutodocKeys.autodocTocForGitHub := true
    )
  ).enablePlugins(play.sbt.PlayScala).aggregate(another)

  // this project enable PlayScala plugin, but add autodocOffSettings so not be applied play-autodoc
  lazy val another = Project(
    "play-scala-another",
    file("another"),
    settings = Defaults.coreDefaultSettings ++
      com.krrrr38.play.autodoc.AutodocPlugin.autodocOffSettings ++
      Seq(
        scalaVersion := "2.10.5",
        libraryDependencies ++= dependencies
      )
  ).enablePlugins(play.sbt.PlayScala)
}

