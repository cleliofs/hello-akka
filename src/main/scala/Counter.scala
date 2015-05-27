import Counter.{Get, Incr}
import akka.actor.{Actor, Props}
import akka.event.LoggingReceive

/**
 * This is a test using Actor based counter.
 *
 * Created by clelio on 27/05/15.
 */
class Counter extends Actor {

  def counter(n: Int): Receive = LoggingReceive {
    case Incr() => context.become(counter(n + 1))
    case Get() => sender ! n
  }

  def receive: Receive = counter(0)
}

object Counter {

  case class Incr()

  case class Get()

}

class CounterMain extends Actor {
  // creates a Counter actor named "counter"
  val counter = context.actorOf(Props[Counter], "counter")

  counter ! Incr()
  counter ! Incr()
  counter ! Incr()
  counter ! Incr()
  counter ! Incr()
  counter ! Incr()
  counter ! Get()

  // receives the messages from Counter
  def receive = LoggingReceive {
    case count: Int =>
      println(s"count was $count")
      context.stop(self)
  }
}


