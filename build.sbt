name := "tokenfsm"

version := "1.0"

scalaVersion := "2.11.8"

version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim

sbtPlugin := true

scalacOptions ++= List("-unchecked")

publishMavenStyle := false

licenses += ("Unlicense", url("http://unlicense.org/"))

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.jcenterRepo
)

libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
