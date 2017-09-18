name := """gestalt-security-sdk-scala"""

organization := "com.galacticfog"

version := "2.4.1"

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](
      name, version, scalaVersion, sbtVersion,
      "builtBy" -> System.getProperty("user.name"),
      "gitHash" -> new java.lang.Object(){
              override def toString(): String = {
                      try { 
                    val extracted = new java.io.InputStreamReader(
                              java.lang.Runtime.getRuntime().exec("git rev-parse HEAD").getInputStream())                         
                    (new java.io.BufferedReader(extracted)).readLine()
                      } catch {      case t: Throwable => "get git hash failed"    }
              }}.toString()
    ),
    buildInfoPackage := "com.galacticfog.gestalt.security.sdk"
  )

scalaVersion := "2.11.7"

isSnapshot := true

scalacOptions ++= Seq(
  "-unchecked", "-deprecation", "-feature",
  "-language:postfixOps", "-language:implicitConversions"
)

resolvers ++= Seq(
  "gestalt-snapshots" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-snapshots-local",
  "gestalt-releases" at "https://galacticfog.artifactoryonline.com/galacticfog/libs-releases-local",
  "snapshots" at "http://scala-tools.org/repo-snapshots",
  "releases"  at "http://scala-tools.org/repo-releases"
)

publishTo <<= version { (v: String) =>
  val ao = "https://galacticfog.artifactoryonline.com/galacticfog/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("publish-gf-snapshots" at ao + "libs-snapshots-local;build.timestamp=" + new java.util.Date().getTime)
  else
    Some("publish-gf-releases"  at ao + "libs-releases-local")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

//
// Adds project name to prompt like in a Play project
//
shellPrompt in ThisBuild := { state => "\033[0;36m" + Project.extract(state).currentRef.project + "\033[0m] " }

// ----------------------------------------------------------------------------
// Play JSON/WS
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
    "com.galacticfog" %% "gestalt-play-json" % "0.4.0",
	"de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % "test",
	specs2 % Test,
    ws
)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

// ----------------------------------------------------------------------------
// Specs 2
// ----------------------------------------------------------------------------

libraryDependencies ++= Seq(
    "junit" % "junit" % "4.12" % "test",
    "org.specs2" %% "specs2-junit" % "2.4.17" % "test",
    "org.specs2" %% "specs2-core" % "2.4.17" % "test"
)
