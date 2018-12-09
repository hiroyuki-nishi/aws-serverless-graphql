import sbt.Keys._
import sbt._
import sbtrelease.Version

name := "aws-serverless-graphql"
resolvers += Resolver.sonatypeRepo("public")
scalaVersion := "2.12.6"
releaseNextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse("Error") }

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings")

val commonLibraryDependencies = Seq(
  "com.amazonaws" % "aws-lambda-java-events" % "1.3.0",
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "io.spray" %%  "spray-json" % "1.3.5"
)

lazy val root = (project in file(".")).aggregate(person)

lazy val domain = (project in file("domain"))

lazy val person = (project in file("application/person"))
  .settings(
    libraryDependencies ++= commonLibraryDependencies
  )
  .settings(assemblyJarName in assembly := "person.jar")
  .dependsOn(domain, infrastracture)

lazy val infrastracture = (project in file("infrastracture"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.0",
      "de.heikoseeberger" %% "akka-http-circe" % "1.20.0",
      "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.29",
      "org.sangria-graphql" %% "sangria" % "1.4.0",
      "org.sangria-graphql" %% "sangria-circe" % "1.2.1",
      "org.sangria-graphql" %% "sangria-spray-json" % "1.0.1",
      "io.circe" %%	"circe-core" % "0.9.2",
      "io.circe" %% "circe-parser" % "0.9.2",
      "io.circe" %% "circe-optics" % "0.9.2",
      "ch.megard" %% "akka-http-cors" % "0.3.1"
    )
  ).dependsOn(domain)
