import akka.actor._
import akka.util.Timeout
import com.typesafe.config._
import akka.pattern.ask
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.language.postfixOps

object RemoteSystem extends App  {

  val conf = ConfigFactory.load()
  val system = ActorSystem("RemoteSystem")
  val actor = system.actorOf(Props(new RemoteServer()), conf.getString("sw.name"))
  val r = scala.util.Random
  implicit val timeout = Timeout(120 seconds)

  if (conf.getBoolean("sw.init")) {
    actor ! Ping(1)
    actor ! Pong(-1)
  }

  while (true) {
    Thread.sleep(r.nextInt(10)*10000)
    val req = actor ? CsRequest()
    Await.result(req, Duration.Inf)
    //critical section
    Thread.sleep(r.nextInt(10)*5000)
    actor ! CsRelease()
  }

}
