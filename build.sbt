name := """gestalt-security-sdk-scala"""

organization := "com.galacticfog"

version := "0.1.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.4"

publishTo := Some("Artifactory Realm" at "http://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq(
  "gestalt" at "http://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases"  at "http://scala-tools.org/repo-releases")

libraryDependencies += "com.galacticfog" % "gestalt-io_2.11" % "1.0-SNAPSHOT"

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

// ----------------------------------------------------------------------------
// Play JSON/WS
// ----------------------------------------------------------------------------

libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.4.0-M2"

libraryDependencies ++= Seq(
    ws
)

// MockWS for testing
libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "0.13" % "test"

// ----------------------------------------------------------------------------
// Specs 2
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.12" % "test",
    "org.specs2" % "specs2-junit_2.11" % "2.4.17" % "test",
    "org.specs2" %% "specs2-core" % "2.4.17" % "test"
)
