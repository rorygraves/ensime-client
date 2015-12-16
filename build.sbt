name := "Ensime Client"

version := "1.0"

scalaVersion := "2.11.7"

lazy val logback = Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "jul-to-slf4j" % "1.7.12",
  "org.slf4j" % "jcl-over-slf4j" % "1.7.12"
)

libraryDependencies ++= Seq(
  "org.ensime" %% "jerk" % "0.9.10-SNAPSHOT",
  "org.suecarter" % "simple-spray-websockets_2.11" % "1.0",
  "com.lihaoyi" %% "ammonite-ops" % "0.5.1",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
) ++ logback

resolvers += Resolver.sonatypeRepo("public")

