name := "client"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

organization := "org.ensime"

lazy val logback = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "jul-to-slf4j" % "1.7.12",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.12"
)

libraryDependencies ++= Seq(
  "org.ensime" %% "jerky" % "0.9.10-SNAPSHOT",
  "org.suecarter" % "simple-spray-websockets_2.11" % "1.0",
  "com.lihaoyi" %% "ammonite-ops" % "0.5.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
) ++ logback

resolvers += Resolver.sonatypeRepo("public")

