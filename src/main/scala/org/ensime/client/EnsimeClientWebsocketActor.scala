package org.ensime.client

import akka.actor.ActorRef
import org.ensime.api._
import org.ensime.jerk.JerkEnvelopeFormats
import org.ensime.jerk.JerkFormats
import org.slf4j.LoggerFactory
import org.suecarter.websocket.WebSocketClient
import spray.can.websocket.UpgradedToWebSocket
import spray.can.websocket.frame.TextFrame

class EnsimeClientWebSocketActor(host: String, port: Int, path: String) extends WebSocketClient(host, port, path) {
  val logger = LoggerFactory.getLogger("EnsimeClientWebSocketActor")

  var nextId = 1
  import JerkFormats._
  import JerkEnvelopeFormats._
  import spray.json._

  private var requests = Map[Int,ActorRef]()
  private var connectionInfo: Option[ConnectionInfo] = None
  private var connectionInfoRequestors: List[ActorRef] = Nil


  def handleRPCResponse(id: Int, payload: EnsimeServerMessage) = {
    requests.get(id) match {
      case Some(ref) =>
        requests -= id
        logger.info("Got response for request " + id)
        ref ! payload
      case _ =>
        logger.warn(s"Got response without requester $id -> $payload")
    }
  }

  def handleAsyncMessage(payload: EnsimeServerMessage) = {
    logger.info(s"GOT $payload")
  }

  def websockets: Receive = {
    case UpgradedToWebSocket =>
      logger.info("websocket connected")
      val json = RpcRequestEnvelope(ConnectionInfoReq, 0).toJson.prettyPrint
      requests += (0 -> self)
      connection ! TextFrame(json)
    case c: ConnectionInfo =>
      connectionInfo = Some(c)
      connectionInfoRequestors.foreach( _ ! c)
      connectionInfoRequestors = Nil
    case ConnectionInfoReq =>
      connectionInfo match {
        case Some(ci) =>
          sender ! ci
        case None =>
          connectionInfoRequestors ::= sender
      }
    case r: RpcRequest =>
      val id = nextId
      nextId += 1
      requests += (id -> sender)
      val env = RpcRequestEnvelope(r, id)
      logger.info(s"Sending $env")
      val json = env.toJson.prettyPrint
      connection ! TextFrame(json)
    case t : TextFrame =>
      val payload = t.payload.utf8String
      val msg = payload.parseJson.convertTo[RpcResponseEnvelope]
      msg.callId match {
        case Some(id) =>
          handleRPCResponse(id, msg.payload)
        case None =>
          handleAsyncMessage(msg.payload)
      }
    case x =>
      logger.info("Got unknown ping message: " + x.getClass + "  " + x)
  }
}
