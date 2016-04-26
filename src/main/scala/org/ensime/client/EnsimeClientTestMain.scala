package org.ensime.client

import akka.actor.ActorSystem
import ammonite.ops._
import org.ensime.api.SourceFileInfo
import org.slf4j.LoggerFactory

object EnsimeClientTestMain {
  val logger = LoggerFactory.getLogger("EnsimeClientTestMain")

  import EnsimeClientHelper._

  def main(args: Array[String]): Unit = {

    logger.info("EnsimeClientTestMain started")
    val system = ActorSystem()
    val projectPath = Path("/workspace/ensime-server/")
    startServer(system, projectPath, MemoryConfig(), skipCreate = true)
    val (_, api) = connectToServer(system, projectPath)
    logger.info("Connection ready - requesting info")
    val ci = api.connectionInfo()
    logger.info(s"Connection Info received $ci")
    val ci2 = api.connectionInfo()
    val srcPath = projectPath / "api" / "src" / "main" / "scala"
    val files = ls.rec ! srcPath |? (_.ext == "scala")
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


  def startServer(actorSystem: ActorSystem, projectPath: Path, memoryConfig: MemoryConfig, skipCreate: Boolean = false): Unit = {

    logger.info("Initialising ensime")
    val wsc = new EnsimeServerStartup(actorSystem, projectPath, memoryConfig)
    if (!skipCreate)
      wsc.create()
    logger.info("Starting server")
    wsc.startServer()
    logger.info("Project startup complete")
  }

}
