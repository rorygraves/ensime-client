package org.ensime.client

import ammonite.ops._
import org.slf4j.LoggerFactory

import scala.concurrent.Future

class EnsimeSetup(projectRoot: Path) {

  val logger = LoggerFactory.getLogger("RobotMain")

  val dotEnsimeFile= projectRoot / ".ensime"
  val cacheDir = projectRoot / ".ensime_cache"
  val resolutionDir = cacheDir / "Resolution"
  val resolutionProjectDir = resolutionDir / "project"
  val resolutionSBTFile = resolutionDir / "build.sbt"
  val classpathFile = resolutionDir / "classpath"
  val resolutionBuildPropertiesFile = resolutionProjectDir / "build.properties"


  val scalaVersion = "2.11.7"
  val ensimeVersion = "0.9.10-SNAPSHOT"
  def projectBuildProps = "sbt.version=0.13.9\n"



  def startServer(): Unit = {
    logger.info("Starting ensime server")
    val classpath = read! classpathFile
    // TODO Need to make this async - and forward the IO output %% does not support this right now
    // TODO Override logger to log to a file?
    Future {
      %%("java", "-Densime.config=" + dotEnsimeFile
        , "-classpath", classpath, "-Densime.explode.on.disconnect=true", "org.ensime.server.Server")(cacheDir)
    }


  }


  def create(): Unit = {

    logger.info("Creating workspace")

    mkdir! resolutionDir
    mkdir! resolutionProjectDir
    write(resolutionSBTFile, sbtClasspathScript(classpathFile))
    write(resolutionBuildPropertiesFile, projectBuildProps)


    logger.info("Running save classpath")
    %%("sbt","saveClasspath")(resolutionDir)
    logger.info("Running gen-ensime")
    %%("sbt","gen-ensime")(projectRoot)


    logger.info("Workspace creation complete")
  }


  def sbtClasspathScript(classpathFile: Path) = s"""
     |import sbt._
     |import IO._
     |import java.io._
     |
     |scalaVersion := "$scalaVersion"
     |
     |ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }
     |
     |// we don't need jcenter, so this speeds up resolution
     |fullResolvers -= Resolver.jcenterRepo
     |
     |// allows local builds of scala
     |resolvers += Resolver.mavenLocal
     |
     |// for java support
     |resolvers += "NetBeans" at "http://bits.netbeans.org/nexus/content/groups/netbeans"
     |
     |// this is where the ensime-server snapshots are hosted
     |resolvers += Resolver.sonatypeRepo("snapshots")
     |
     |libraryDependencies += "org.ensime" %% "ensime" % "$ensimeVersion"
     |
     |dependencyOverrides ++= Set(
     |   "org.scala-lang" % "scala-compiler" % scalaVersion.value,
     |   "org.scala-lang" % "scala-library" % scalaVersion.value,
     |   "org.scala-lang" % "scala-reflect" % scalaVersion.value,
     |   "org.scala-lang" % "scalap" % scalaVersion.value
     |)
     |val saveClasspathTask = TaskKey[Unit]("saveClasspath", "Save the classpath to a file")
     |saveClasspathTask := {
     |   val managed = (managedClasspath in Runtime).value.map(_.data.getAbsolutePath)
     |   val unmanaged = (unmanagedClasspath in Runtime).value.map(_.data.getAbsolutePath)
     |   val out = file("$classpathFile")
     |   write(out, (unmanaged ++ managed).mkString(File.pathSeparator))
     |}
     |""".stripMargin
}
