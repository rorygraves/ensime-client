package org.ensime.client

import java.io.{InputStreamReader, BufferedReader, InputStream}

import akka.actor.ActorSystem
import ammonite.ops._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class EnsimeServerStartup(actorSystem: ActorSystem, projectRoot: Path) {

  implicit val ec: ExecutionContext = actorSystem.dispatcher
  val logger = LoggerFactory.getLogger("EnsimeServer")

  val dotEnsimeFile= projectRoot / ".ensime"
  val cacheDir = projectRoot / ".ensime_cache"

  val httpPortFile = cacheDir / "http"
  val tcpPortFile = cacheDir / "port"

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

    val javaHome = sys.env.get("JAVA_HOME")
    val toolsJar = javaHome match {
      case Some(path) =>
        val toolsJarPath = Path(path) / "lib" / "tools.jar"
        if(!exists(toolsJarPath))
          throw new IllegalArgumentException(s"Cannot resolve tools jar from JAVA_HOME - expecting $toolsJarPath")
        toolsJarPath
      case None =>
        throw new IllegalStateException("JAVA_HOME not set")
    }

    val baseClasspath = read ! classpathFile
    val classpath = s"$toolsJar:$baseClasspath"
    Future {
      val result = Command(Vector.empty, Map.empty, logOutputStreamer)("java", "-Densime.config=" + dotEnsimeFile
        , "-classpath", classpath, "-Densime.explode.on.disconnect=true", "org.ensime.server.Server")(cacheDir)
      logger.info(s"start server completed with exitCode: ${result.exitCode}")
    }
  }

  def streamLogger(inputStream: InputStream, opTag: String): Unit = {

    Future {
      val is = new BufferedReader(new InputStreamReader(inputStream))
      var line = is.readLine()
      while(line != null) {
        logger.info(s"$opTag - $line")
        line = is.readLine()
      }
    }
  }

  def logOutputStreamer(wd: Path, cmd: Command[_]) = {
    val builder = new java.lang.ProcessBuilder()
    import collection.JavaConversions._
    builder.environment().putAll(cmd.envArgs)
    builder.directory(new java.io.File(wd.toString))
    val process =
      builder
        .command(cmd.cmd:_*)
        .start()
    val stdout = process.getInputStream
    val stderr = process.getErrorStream
    streamLogger(stdout, "out")
    streamLogger(stderr, "err")
    val chunks = collection.mutable.Buffer.empty[Either[Bytes, Bytes]]
    while(
    // Process.isAlive doesn't exist on JDK 7 =/
      util.Try(process.exitValue).isFailure
    ){
      Thread.sleep(1000)
    }

    val res = CommandResult(process.exitValue(), chunks)
    if (res.exitCode == 0) res
    else throw ShelloutException(res)
  }


  def create(): Unit = {

    logger.info("Creating workspace")

    mkdir! resolutionDir
    mkdir! resolutionProjectDir
    write.over(resolutionSBTFile, sbtClasspathScript(classpathFile))
    write.over(resolutionBuildPropertiesFile, projectBuildProps)


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
