name := "social_group_feed"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint", "-opt:l:inline", "-opt-inline-from:**", "-Ypartial-unification", "-language:higherKinds")

javacOptions ++= Seq("-Xlint")

val scalaTestVersion = "3.0.5"
val log4jVersion = "2.11.2"

libraryDependencies ++= Seq(
	"org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0",
	"org.scalactic" %% "scalactic" % scalaTestVersion,
	"org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

libraryDependencies ++= Seq(
	"org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
	"org.apache.logging.log4j" % "log4j-api" % log4jVersion,
	"org.apache.logging.log4j" % "log4j-core" % log4jVersion,
	"org.apache.logging.log4j" %% "log4j-api-scala" % "11.0"
)

Test / parallelExecution := false
