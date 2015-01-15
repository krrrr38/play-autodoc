Autodoc for Play 2.x
====

Play 2.x Scala port of [autodoc](https://github.com/r7kamura/autodoc/)

**Now only support Play 2.3.x**

## Description
Generate documentation from your Play application.

- See example
  - [Generated Document](https://github.com/krrrr38/play-autodoc/blob/master/example/doc/Users.md)
  - [Autodoc Test](https://github.com/krrrr38/play-autodoc/blob/master/example/test/UsersSpec.scala)

## Install with sbt plugin
Use `AutodocPlugin`(sbt plugin) to use play-autodoc-core. It provides a custom configuration for generating documents.

- Setup sbt plugin and play-autodoc configuration

`project/plugins.sbt`
```scala
resolvers += "Maven Repository on Github" at "http://krrrr38.github.io/maven/"

addSbtPlugin("com.krrrr38" % "play-autodoc-sbt" % "0.0.1")
```

## Configuration
In `build.sbt` / `project/Build.scala`, if you want change setting

```scala
import com.krrrr38.play.autodoc.AutodocPlugin.AutodocKeys

...
  settings = Seq(
    AutodocKeys.autodocOutputDirectory := "doc", // default "doc"
    AutodocKeys.autodocSuppressedRequestHeaders := Seq("X-Secret-Token"), // default Nil
    AutodocKeys.autodocSuppressedResponseHeaders := Nil // default Nil
  )
)
```

## Usage
- **In test**, add `import com.krrrr38.play.autodoc.AutodocHelpers._`, add `Caller` implicit, and write normal scenario tests like following.
  - you can use `autodoc` to annotate test for generating documents

```scala
import com.krrrr38.play.autodoc.AutodocHelpers._

implicit val caller = Caller(this.getClass)

"return user list" in new WithApplication {
  val res = autodoc("GET /api/users", "get all users")
    .route(FakeRequest(GET, "/api/users")).get
  status(res) must equalTo(OK)
  ...
}

"create user" in new WithApplication {
  val res = autodoc("POST /api/users", "create user")
    .route(
      FakeRequest(POST, "/api/users")
        .withHeaders("X-API-Token" -> "")
        .withJsonBody(Json.obj("name" -> "yuno", "height" -> 144))
    ).get
  status(res) must equalTo(CREATED)
```

Just add `autodoc(title, description)`, before `route`.

(`AutodocHelpers` trait is also existed, so you can mixin into your test helper object.)

- To generate documents, run `autodoc:test` or `autodoc:testOnly ...`

On `PlayScala` activated project, you can use `autodoc` configuration which extends `Test` configuration.

`autodoc:test` and `autodoc:testOnly` executes play application tests and generate document on project directory.

## Minority Usage
**If you use AutodocPlugin, you can ignore this section.**

You need not to use `AutodocPlugin`. To use play-autodoc directory, just add dependency.

`build.sbt`
```
resolvers += "Maven Repository on Github" at "http://krrrr38.github.io/maven/"

libraryDependencies += "com.krrrr38" % "play-autodoc-core" % "0.0.1"
```

To generate document, set java option `-Dplay.autodoc=true` then run test.

If you use `AutodocPlugin`, `play-autodoc-core` would be automatically added into your libraryDependencies.

## Contribution

1. Fork ([https://github.com/krrrr38/play-autodoc/fork](https://github.com/krrrr38/play-autodoc/fork))
1. Create a feature branch
1. Commit your changes
1. Rebase your local changes against the master branch
1. Run test suite with the `sbt test` command and confirm that it passes
1. Create new Pull Request

## Author

[krrrr38](https://github.com/krrrr38)
