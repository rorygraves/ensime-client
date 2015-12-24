package org.ensime.client

import akka.actor.{ActorRef, Props, ActorSystem}
import ammonite.ops.{read, exists, Path}
import org.slf4j.LoggerFactory

object EnsimeClientHelper {
  val logger = LoggerFactory.getLogger("EnsimeClientHelper")

  def connectToServer(system: ActorSystem,path: Path): (ActorRef,EnsimeApi) = {
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
    (actorRef, api)
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
}
