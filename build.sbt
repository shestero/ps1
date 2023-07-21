ThisBuild / version := "0.2.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

lazy val root = (project in file("."))
  .settings(
    name := "ps1"
  )

// Scala modules
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "2.1.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

// Cats
val catsVersion = "3.4.8"
libraryDependencies += "org.typelevel" %% "cats-effect" % catsVersion

// Akka
val AkkaVersion = "2.8.1"
val AkkaHttpVersion = "10.5.2"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
)

// Tapir
val TapirVersion = "1.5.5"
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % TapirVersion exclude("com.typesafe.akka", "akka-stream_2.12"),
  // Otherwise this will transitively pull some Akka modules in version 2.6. ^^^
  "com.softwaremill.sttp.tapir" %% "tapir-files" % TapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % TapirVersion,
  "com.softwaremill.sttp.shared" %% "akka" % "1.3.15"
)

// RDMB
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "6.0.1"
libraryDependencies += "org.postgresql" % "postgresql" % "42.6.0"
libraryDependencies += "com.github.tminglei" %% "slick-pg" % "0.21.1" // "0.22.0-M3"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.4" // need to postgres arrays

// Sttp
libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.8.15"

// Note
// https://github.com/tminglei/slick-pg
// https://github.com/tminglei/slick-pg/blob/5ff8dc17e2d32b477196186cd7fe2e96b317c176/core/src/main/scala/com/github/tminglei/slickpg/json/PgJsonExtensions.scala#L56

scalacOptions += "-Ymacro-annotations"
