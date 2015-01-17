import sbt._
import Keys._

import com.krrrr38.play.autodoc.AutodocPlugin.AutodocKeys

object PlayScalaBuild extends Build {
  lazy val root = Project(
    "play-scala",
    file("."),
    settings = Defaults.coreDefaultSettings ++ Seq(
      scalaVersion := "2.10.4",
      crossScalaVersions := scalaVersion.value :: "2.11.4" :: Nil,
      scalacOptions ++= (
        "-deprecation" ::
          "-feature" ::
          "-unchecked" ::
          "-Xlint" ::
          Nil
        ),
      AutodocKeys.autodocOutputDirectory := "doc",
      AutodocKeys.autodocSuppressedRequestHeaders := Seq("X-Secret-Token"),
      AutodocKeys.autodocSuppressedResponseHeaders := Nil
    )
  ).enablePlugins(play.PlayScala).aggregate(another)

  // this project enable PlayScala plugin, but add autodocOffSettings so not be applied play-autodoc
  lazy val another = Project(
    "play-scala-another",
    file("another"),
    settings = Defaults.coreDefaultSettings ++
      com.krrrr38.play.autodoc.AutodocPlugin.autodocOffSettings ++
      Seq(
        scalaVersion := "2.10.4"
      )
  ).enablePlugins(play.PlayScala)
}

