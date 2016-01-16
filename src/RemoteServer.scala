import akka.actor._
import com.typesafe.config._

class RemoteServer extends Actor {

  val conf = ConfigFactory.load()
  val next = context.system.actorSelection(conf.getString("sw.next"))
  val r = scala.util.Random
  val DEBUG = conf.getBoolean("sw.debug")
  val pingloss = conf.getBoolean("sw.pingloss")
  val pongloss = conf.getBoolean("sw.pongloss")

  context.become(receive(0))

  def debug(s: String) = if (DEBUG) println(s)

  def receive = {case _ =>}

  def receive(m: Int) : Receive = {

    case Ping(value) =>
      debug("Received PING " + value)
      if (m == value) {
        debug("Regenerate lost PONG, sending to next")
        next ! Pong(-value)
      }
      forwardPing(value)
      context.become(receive(value))

    case Pong(value) =>
      debug("Received PONG "+value)
      if (m == value) {
        debug("Regenerate lost PING and forward it")
        next ! Ping(-value)
      }
      forwardPong(value)
      context.become(receive(value))

    case CsRequest() =>
      debug("Requesting critical section")
      context.become(receiveInCsRequest(m, sender))

  }
  def receiveInCsRequest(m: Int, sender: ActorRef) : Receive = {

    case Ping(value) =>
      debug("Received PING "+value)
      if (m == value)
        debug("Regenerate lost PONG and forward it")
        next ! Pong(-value)
      debug("Entering critical section")
      sender ! CsResponse()
      context.become(receiveInCs(value))

    case Pong (value) =>
      debug("Received PONG "+value)
      if (m == value) {
        val newVal = value - 1
        debug("Regenerate lost PING and Entering critical section")
        debug("Meeting PING and PONG, increasing value")
        sender ! CsResponse()
        context.become(receiveInCs(newVal))
        forwardPong(newVal)
      } else {
        forwardPong(value)
        context.become(receiveInCsRequest(value, sender))
      }

  }
  def receiveInCs(m: Int) : Receive = {

    case Pong (value) =>
      debug("Received PONG "+value)
      debug("Meeting PING and PONG, increasing value")
      val newVal = value - 1
      forwardPong(value)
      context.become(receiveInCs(newVal))

    case CsRelease() =>
      debug("Leaving critical section")
      val newVal = Math.abs(m)
      forwardPing(newVal)
      context.become(receive(newVal))

  }

  def forwardPing(value: Int) = {
    Thread.sleep(200)
    if (pingloss && r.nextInt(10) < 3) {
      debug("PING lost...")
    } else {
      debug("Forward PING " + value)
      next ! Ping(value)
    }
  }

  def forwardPong(value: Int) = {
    Thread.sleep(200)
    if (pongloss && r.nextInt(10) < 3) {
      debug("PONG lost...")
    } else {
      debug("Forward PONG " + value)
      next ! Pong(value)
    }
  }

}
