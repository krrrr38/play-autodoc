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
  ).enablePlugins(play.PlayScala)
}

