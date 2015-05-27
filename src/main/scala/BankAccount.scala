import BankAccount.{Failed, Done, Withdraw, Deposit}
import WireTransfer.Transfer
import akka.actor.{Props, ActorRef, Actor}
import akka.event.LoggingReceive

/**
 * BankAccount actor example
 *
 * Created by clelio on 27/05/15.
 */
class BankAccount extends Actor {

  var balance: BigDecimal = BigDecimal(0)

  def receive = LoggingReceive {
    case Deposit(amount) =>
      balance += amount
      sender ! Done
    case Withdraw(amount) if amount <= balance => {
      balance -= amount
      sender ! Done
    }
    case _ => sender ! Failed
  }

}

object BankAccount {
  case class Deposit(amount: BigDecimal) {
    require(amount > 0)
  }
  case class Withdraw(amount: BigDecimal) {
    require(amount > 0)
  }

  case object Done
  case object Failed
}


class WireTransfer extends Actor {

  def receive: Receive = LoggingReceive {
    case Transfer(from, to, amount) =>
      from ! BankAccount.Withdraw(amount)
      context.become(awaitFrom(to, amount, sender))
  }

  def awaitFrom(to: ActorRef, amount: BigDecimal, customer: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      to ! BankAccount.Deposit(amount)
      context.become(awaitTo(customer))
    case BankAccount.Failed =>
      customer ! WireTransfer.Failed
      context.stop(self)
  }

  def awaitTo(customer: ActorRef): Receive = LoggingReceive {
    case BankAccount.Done =>
      customer ! WireTransfer.Done
      context.stop(self)
  }
}

object WireTransfer {
  case class Transfer(from: ActorRef, to: ActorRef, amount: BigDecimal)
  case object Done
  case object Failed
}


class TransferMain extends Actor {

  val accountA = context.actorOf(Props[BankAccount], "accountA")
  val accountB = context.actorOf(Props[BankAccount], "accountB")

  accountA ! BankAccount.Deposit(100)

  def receive = LoggingReceive {
    case BankAccount.Done => transfer(150)
  }

  def transfer(amount: BigDecimal): Unit = {
    val transaction = context.actorOf(Props[WireTransfer], "transfer")
    transaction ! WireTransfer.Transfer(accountA, accountB, amount)
    context.become(LoggingReceive {
      case WireTransfer.Done =>
        println("success")
        context.stop(self)
      case WireTransfer.Failed =>
        println("failed")
        context.stop(self)
    })
  }
}