ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "Dixa"
  )
  .aggregate(contract, proxyServer, primeNumberServer)

lazy val contract =
  (project in file("contract")).enablePlugins(AkkaGrpcPlugin)

lazy val proxyServer = (project in file("proxy-server"))
  .settings(
    name := "proxy-server",
    libraryDependencies ++= Seq (
      dependencies.akkaActor,
      dependencies.akkaHttp,
      dependencies.akkaStream,
      dependencies.akkaHttpCirce,
      dependencies.circeCore,
      dependencies.circeParser,
      dependencies.circeGeneric,
      dependencies.log4jCore,
      dependencies.log4jAPI,
      dependencies.log4jSLF4jImpl
    )
  )
  .dependsOn(contract)

lazy val primeNumberServer = (project in file("prime-number-server"))
  .settings(
    name := "prime-number-server",
    libraryDependencies ++= Seq(
      dependencies.akkaActor,
      dependencies.log4jCore,
      dependencies.log4jAPI,
      dependencies.log4jSLF4jImpl
    )
  )
  .dependsOn(contract)

lazy val dependencies =
  new {
    val AkkaVersion = "2.6.9"
    val AkkaHttpVersion = "10.2.7"
    val log4jVersion = "2.17.1"
    val circeVersion = "0.14.1"
    val akkaHttpJsonVersion = "1.40.0-RC1"

    val akkaActor = "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
    val akkaHttp = "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
    val akkaHttpCirce = "de.heikoseeberger" %% "akka-http-circe" % akkaHttpJsonVersion
    val log4jCore = "org.apache.logging.log4j" % "log4j-core" % log4jVersion
    val log4jAPI = "org.apache.logging.log4j" % "log4j-api" % log4jVersion
    val log4jSLF4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
    val circeParser = "io.circe" %% "circe-parser" % circeVersion
    val circeCore = "io.circe" %% "circe-core" % circeVersion
    val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  }