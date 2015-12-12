package org.ensime.client

import ammonite.ops._
import org.slf4j.LoggerFactory

object EnsimeClientTestMain {
  val logger = LoggerFactory.getLogger("RobotMain")

  def main(args: Array[String]): Unit = {
    val path = TestProjectCreator.createTestProject("2.11.7")

    logger.info("EnsimeClientTestMain started")
    startServer(path)
  }


  def startServer(projectPath: Path): Unit = {

    logger.info("Initialising ensime")
    val wsc = new EnsimeSetup(projectPath)
    wsc.create()
    logger.info("Starting server")
    wsc.startServer()
    logger.info("Project startup complete")
  }
}
