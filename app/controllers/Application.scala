package controllers

import akka.actor._
import akka.pattern.pipe
import play.api._
import play.api.libs.concurrent.Promise
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  // client cannot send a chunk until it gets a "next" message from the server
  // if it does, we close the connection
  def upload = WebSocket.acceptWithActor[Array[Byte], String] { _ => out =>
    Props(new UploadWithAckActor(out))
  }

}

class UploadWithAckActor(out: ActorRef) extends Actor {


  import context.dispatcher

  case object ChunkDone

  Logger.info("WebSocket opened")

  def receive = open

  def open: Actor.Receive = {
    out ! "next"
    ({
      case data: Array[Byte] =>
        Logger.info(s"Got chunk (${data.size} bytes)")

        SomeBackendService.doSomethingWithTheData(data)
          .map(_ => ChunkDone)
          .pipeTo(self)
        context.become(closed)
    }: Actor.Receive)
  }

  def closed: Actor.Receive = {

    case ChunkDone => context.become(open)

    case _ =>
      Logger.info("Got data when busy, big nono, closing connection")
      out ! "Shame on you sending data when you shouldn't"
      out ! PoisonPill

  }

  override def postStop() = {
    Logger.info("WebSocket closed")
  }
}

object SomeBackendService {

  import scala.concurrent.duration._
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def doSomethingWithTheData(data: Any): Future[Unit] =
    // just fake doing something that takes a while with the data
    Promise.timeout((), 1.second)

}