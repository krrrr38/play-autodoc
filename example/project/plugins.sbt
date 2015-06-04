resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven Repository on Github" at "http://krrrr38.github.io/maven/"
)

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0")

addSbtPlugin("com.krrrr38" % "play-autodoc-sbt" % "0.2.0")
