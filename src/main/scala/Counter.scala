import Counter.{Get, Incr}
import akka.actor.{SupervisorStrategy, OneForOneStrategy, Actor, Props}
import akka.event.LoggingReceive

/**
 * This is a test using Actor based counter.
 *
 * Created by clelio on 27/05/15.
 */
class Counter extends Actor {

  def counter(n: Int): Receive = LoggingReceive {
    case Incr => context.become(counter(n + 1))
    case Get => if (n == 0) throw new UnsupportedOperationException else sender ! n
  }

  def receive: Receive = counter(0)
}

object Counter {

  case object Incr

  case object Get

}

class CounterMain extends Actor {
  // creates a Counter actor named "counter"
  val counter = context.actorOf(Props[Counter], "counter")

  // make the child actor to fail once the counter is 0
  counter ! Get

  counter ! Incr
  counter ! Incr
  counter ! Incr
  counter ! Incr
  counter ! Incr
  counter ! Incr

  counter ! Get

  // receives the messages from Counter
  def receive = LoggingReceive {
    case count: Int =>
      println(s"count was $count")
      context.stop(self)
  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
    case _: Exception => SupervisorStrategy.Restart
  }

}


