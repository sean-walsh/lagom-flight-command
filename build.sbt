lazy val root = (project in file("."))
  .settings(name := "reactiveair-flight-scala")
  .aggregate(flightApi, flightImpl)

organization in ThisBuild := "com.reactiveair"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.11.8"

val playJsonDerivedCodecs = "org.julienrf" %% "play-json-derived-codecs" % "3.3"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

lazy val flightApi = (project in file("flight-api"))
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      playJsonDerivedCodecs
    )
  )

lazy val flightImpl = (project in file("flight-impl"))
  .enablePlugins(LagomScala)
  .dependsOn(flightApi)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslTestKit,
      lagomScaladslKafkaBroker,
      macwire,
      scalaTest
    ),
    maxErrors := 10000
  )
  .settings(lagomForkedTestSettings: _*)

lagomCassandraCleanOnStart in ThisBuild := true
