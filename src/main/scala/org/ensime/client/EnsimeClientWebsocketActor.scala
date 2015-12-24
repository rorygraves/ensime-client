package org.ensime.client

import akka.actor.ActorRef
import org.ensime.api._
import org.ensime.jerk.JerkEnvelopeFormats
import org.slf4j.LoggerFactory
import org.suecarter.websocket.WebSocketClient
import spray.can.websocket.UpgradedToWebSocket
import spray.can.websocket.frame.TextFrame

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext

case class SubscribeEventsReq(actorRef: ActorRef)
case class SubscribeEventsResp(subscribed: Boolean)
case object Heartbeat


class EnsimeClientWebSocketActor(host: String, port: Int, path: String) extends WebSocketClient(host, port, path) {
  val logger = LoggerFactory.getLogger("EnsimeClientWebSocketActor")

  var nextId = 1
  import JerkEnvelopeFormats._
  import spray.json._

  implicit val ec: ExecutionContext = context.system.dispatcher
  private var requests = Map[Int,ActorRef]()
  private var connectionInfo: Option[ConnectionInfo] = None

  var asyncSubscriber: Option[ActorRef] = None
  var unprocessedEvents = Queue[EnsimeEvent]()

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
    payload match {
      case e : EnsimeEvent =>
        asyncSubscriber match {
          case Some(sub) =>
            sub ! e
          case None =>
            unprocessedEvents = unprocessedEvents :+ e
        }
      case _ =>
        logger.error(s"Illegal state - received async message for non-async event: $payload")
    }
  }

  def scheduleHeartbeat(): Unit = {
    import scala.concurrent.duration._
    context.system.scheduler.schedule(15.seconds, 15.seconds, self, Heartbeat)
  }

  def sendToEnsime(rpcRequest: RpcRequest, sender: ActorRef): Unit = {
    val id = nextId
    nextId += 1
    requests += (id -> sender)
    val env = RpcRequestEnvelope(rpcRequest, id)
    logger.info(s"Sending $env")
    val json = env.toJson.prettyPrint
    connection ! TextFrame(json)
  }

  def websockets: Receive = {
    case c: ConnectionInfo =>
      if(connectionInfo.isEmpty)
      connectionInfo = Some(c)
    case Heartbeat =>
      sendToEnsime(ConnectionInfoReq, self)
    case r: RpcRequest =>
      sendToEnsime(r, sender)
    case t : TextFrame =>
      val payload = t.payload.utf8String
      val msg = payload.parseJson.convertTo[RpcResponseEnvelope]
      msg.callId match {
        case Some(id) =>
          handleRPCResponse(id, msg.payload)
        case None =>
          handleAsyncMessage(msg.payload)
      }
    case SubscribeEventsReq(ref) =>
      if(asyncSubscriber.isEmpty) {
        ref ! SubscribeEventsResp(true)
        asyncSubscriber = Some(ref)
        unprocessedEvents.foreach(ref ! _)
        unprocessedEvents = Queue.empty
      } else {
        // we already have a subscriber
        sender ! SubscribeEventsResp(false)
      }

    case UpgradedToWebSocket =>
      sendToEnsime(ConnectionInfoReq, self)
      scheduleHeartbeat()
    case x =>
      logger.info("Got unknown message: " + x.getClass + "  " + x)
  }
}
