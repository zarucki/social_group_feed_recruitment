name := "social_group_feed"

version := "0.1"

scalaVersion := "2.12.8"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint", "-opt:l:inline", "-opt-inline-from:**", "-Ypartial-unification", "-language:higherKinds")

javacOptions ++= Seq("-Xlint")

val scalaTestVersion = "3.0.5"

libraryDependencies ++= Seq(
	"org.mongodb.scala" %% "mongo-scala-driver" % "2.6.0",
	"org.scalactic" %% "scalactic" % scalaTestVersion,
	"org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

Test / parallelExecution := false
