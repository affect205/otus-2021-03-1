import sbt._

object Dependencies {

  object V {
    // Scala
    val decline          = "2.0.0"
    val circe            = "0.14.1"

    val catsEffect       = "3.1.1"
    val catsEffectTest   = "1.1.1"
    val http4sVersion    = "1.0.0-M23"
    val fs2              = "3.0.4"

    // Scala (test only)
    val specs2           = "4.10.5"
    val scalaCheck       = "1.15.1"
  }

  // Scala
  val all = List(
    "com.monovore"               %% "decline"              % V.decline,
    "io.circe"                   %% "circe-core"           % V.circe,
    "io.circe"                   %% "circe-parser"         % V.circe,
    "io.circe"                   %% "circe-generic"        % V.circe,

    "org.typelevel"              %% "cats-effect"          % V.catsEffect,

    "co.fs2"                     %% "fs2-io"               % V.fs2,

    "org.http4s"                 %% "http4s-dsl"           % V.http4sVersion,
    "org.http4s"                 %% "http4s-blaze-server"  % V.http4sVersion,
    "org.http4s"                 %% "http4s-blaze-client"  % V.http4sVersion,

    "org.http4s"                 %% "http4s-circe"         % V.http4sVersion,
    "io.circe"                   %% "circe-literal"        % V.circe,

    "org.slf4j"                   % "slf4j-simple"         % "1.7.36",

    "org.typelevel"              %% "cats-effect-testing-specs2" % V.catsEffectTest % Test,
    "org.specs2"                 %% "specs2-core"                % V.specs2         % Test,
    "org.scalacheck"             %% "scalacheck"                 % V.scalaCheck     % Test
  )
}
