package org.ensime.client

import java.io.File

import akka.actor.{Props, ActorSystem}
import ammonite.ops._
import org.ensime.api.{SourceFileInfo, SourceSymbol}
import org.slf4j.LoggerFactory

object EnsimeClientTestMain {
  val logger = LoggerFactory.getLogger("EnsimeClientTestMain")


  def selfStartServer(system: ActorSystem): Unit = {
    val path = TestProjectCreator.createTestProject("2.11.7")
    startServer(system, path)
    connectToServer(system, path)


  }

  def connectToServer(system: ActorSystem,path: Path): EnsimeApi = {
    logger.info(s"Connecting to server in $path")
    val cachePath = path / ".ensime_cache"
    val httpPortFile =  cachePath / "http"
    val tcpPortFile = cachePath / "port"
    val httpPort = waitForAndReadPort(httpPortFile)
    val tcpPort = waitForAndReadPort(tcpPortFile)

    logger.info(s"Http port resolved to $httpPort")
    val clientProps = Props(classOf[EnsimeClientWebSocketActor],"127.0.0.1", httpPort, "/jerky")
    val actorRef = system.actorOf(clientProps)
    val api = new EnsimeApiImpl(actorRef)
    api
  }

  def waitForAndReadPort(file: Path): Int = {
    var count = 0
    var res: Option[Int] = None
    while(count < 30 && res.isEmpty) {
      if(exists! file) {
        val contents = read ! file
        res = Some(Integer.parseInt(contents.trim))
      } else {
        Thread.sleep(1000)
      }
      count += 1
    }
    res match {
      case Some(p) =>
        p
      case None =>
        throw new IllegalStateException(s"Port file $file not available")
    }
  }

  def main(args: Array[String]): Unit = {

    logger.info("EnsimeClientTestMain started")
    val system = ActorSystem()
//    selfStartServer(system)
    val projectPath = Path("/workspace/ensime-server/")
    startServer(system, projectPath, skipCreate = true)
    val api = connectToServer(system, projectPath)
    logger.info("Connection ready - requesting info")
    val ci = api.connectionInfo()
    logger.info(s"HERE XXX $ci")
    val srcPath = projectPath / "api" / "src" / "main" / "scala"
    val files = ls.rec! srcPath |? (_.ext == "scala")
    files.foreach { f =>
      logger.info("Asking symbols for " + f)
      val file = new java.io.File(f.toString())
  //    val symbols = api.symbolDesignations(file, 0, Integer.MAX_VALUE, SourceSymbol.allSymbols)
      println("   xxxxx - sending request " + f.relativeTo(projectPath))
      api.typecheckFile(SourceFileInfo(file))
      println("   xxxxx - after request")
      Thread.sleep(10000)
    }

  }


  def startServer(actorSystem: ActorSystem, projectPath: Path, skipCreate: Boolean = false): Unit = {

    logger.info("Initialising ensime")
    val wsc = new EnsimeServerStartup(actorSystem, projectPath)
    if(!skipCreate)
      wsc.create()
    logger.info("Starting server")
    wsc.startServer()
    logger.info("Project startup complete")
  }

}
