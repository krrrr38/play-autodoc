Autodoc for Play 2.x
====
[![Build Status](https://travis-ci.org/krrrr38/play-autodoc.svg?branch=2.3.x)](https://travis-ci.org/krrrr38/play-autodoc)
[![Build Status](https://travis-ci.org/krrrr38/play-autodoc.svg?branch=2.4.x)](https://travis-ci.org/krrrr38/play-autodoc)

Play 2.x Scala port of [autodoc](https://github.com/r7kamura/autodoc/)

**only support Play 2.3.x and 2.4.x**

| Play version    | autodoc version |
| :-------------: |:---------------:|
| 2.3.x           | 0.1.1           |
| 2.4.x           | 0.2.0           |

## Description
Generate documentation from your Play application.

- See example
  - [Generated Document](https://github.com/krrrr38/play-autodoc/blob/2.4.x/example/doc/Users.md)
  - [Autodoc Test](https://github.com/krrrr38/play-autodoc/blob/2.4.x/example/test/UsersSpec.scala)

## Install with sbt plugin
Use `AutodocPlugin`(sbt plugin) to use play-autodoc-core. It provides a custom configuration for generating documents.

- Setup sbt plugin and play-autodoc configuration

`project/plugins.sbt`
```scala
addSbtPlugin("com.krrrr38" % "play-autodoc-sbt" % "version")
```

## Configuration
if you want to change setting, write followings in `build.sbt` / `project/Build.scala`...

```scala
import com.krrrr38.play.autodoc.AutodocPlugin.AutodocKeys

...
settings = Seq(
  AutodocKeys.autodocOutputDirectory := "doc", // default "doc"
  AutodocKeys.autodocSuppressedRequestHeaders, := Seq("X-Secret-Token"), // default Nil
  AutodocKeys.autodocSuppressedResponseHeaders := Nil, // default Nil
  AutodocKeys.autodocTocForGitHub := true // default false
)
```

## Usage
- **In test**, add `import com.krrrr38.play.autodoc.AutodocHelpers._`, add `AutodocCaller` implicit, and write normal scenario tests like following.
  - you can use `autodoc` to annotate test for generating documents

```scala
import com.krrrr38.play.autodoc.AutodocHelpers._

implicit val caller = AutodocCaller(this.getClass)

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

## Disable Autodoc
If some PlayScala project `aggregate` other PlayScala project which you don't want to apply play-autodoc, you can prevent play-autodoc in other one to add `autodocOffSettings`.

See example [HERE](https://github.com/krrrr38/play-autodoc/blob/2.4.x/example/project/Build.scala)

## Contribution

1. Fork ([https://github.com/krrrr38/play-autodoc/fork](https://github.com/krrrr38/play-autodoc/fork))
1. Create a feature branch
1. Commit your changes
1. Rebase your local changes against the target version branch
1. Run test suite with the `sbt test` command and confirm that it passes
1. Create new Pull Request

To check with `example` project...

1. Change play-autodoc version in `project/Build.scala`
1. `publishLocal`
1. Change play-autodoc version in `example/project/plugins.sbt`
1. run example project with sbt on `example` directory.

## Author

[krrrr38](https://github.com/krrrr38)
