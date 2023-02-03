package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ActorCapabilities.Person.LiveTheLife

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => sender() ! "Hello, there!" // replying to the message
      case message: String => println(s"[simple actor] I've received: $message")
      case number: Int => println(s"[simple actor] I've received a number: $number")
      case SpecialMessage(contents) => println(s"[simple actor] I've received something special: $contents")
      case SendMessageToYourself(content) =>
        self ! content
      case SayHiTo(ref) => ref ! "Hi!" // alice is being passed as the sender
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s")
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], name = "simpleActor")

  simpleActor ! "hello, actor!"

  // 1 - messages can be of any type
  // a) messages must by IMMUTABLE
  // b) messages must be SERIALIZABLE
  // in practice use case classes and case objects

  simpleActor ! 42 // who is the sender?!

  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("some special content")

  // 2 - actors have information about their context and about themselves
  // `context.self` === `self` === `this` in OOP

  case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("I'm an actor and I'm proud of it")

  // 3 - actor can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], name = "alice")
  val bob = system.actorOf(Props[SimpleActor], name = "bob")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "Hi!" // reply to "me"

  // 5 - forwarding messages
  // D -> A -> B
  // forwarding = sending a message with the ORIGINAL sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi", bob) // no sender

  //@formatter:off
  /**
   * Exercises
   *
   * 1. a Counter actor
   *   - Increment
   *   - Decrement
   *   - Print
   * 2. a Bank account as an actor
   *   receives
   *   - Deposit an amount
   *   - Withdraw an amount
   *   - Statement
   *   replies with
   *   - Success
   *   - Failure
   *
   *   interact with some other kind of actor
   *
   */
  //@formatter:on

  // DOMAIN of the counter
  object Counter {
    case object Increment

    case object Decrement

    case object Print
  }


  class Counter extends Actor {

    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => {
        count += 1
      }
      case Decrement => {
        count -= 1
      }
      case Print => {
        println(s"[counter] My current count is $count")
      }
    }
  }

  import Counter._

  val counter = system.actorOf(Props[Counter], name = "myCounter")
  (1 to 5).foreach(- => counter ! Increment)
  (1 to 3).foreach(- => counter ! Decrement)
  counter ! Print

  object BankAccount {
    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object Statement

    case class TransactionSuccess(message: String)

    case class TransactionFailure(message: String)
  }

  // bank account
  class BankAccount extends Actor {

    import BankAccount._

    var funds = 0

    override def receive: Receive = {
      case Deposit(amount: Int) => {
        if (amount < 0) {
          sender ! TransactionFailure("invalid deposit amount")
        } else {
          funds += amount
          sender ! TransactionSuccess(s"successfully deposited $amount")
        }
      }
      case Withdraw(amount: Int) => {
        if (amount < 0) {
          sender ! TransactionFailure("invalid withdraw amount")
        } else if (amount > funds) {
          sender ! TransactionFailure("insufficient funds")
        } else {
          funds -= amount
          sender ! TransactionSuccess(s"successfully withdrew $amount")
        }
      }
      case Statement => {
        sender ! s"Your balance is $funds"
      }
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {

    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account: ActorRef) => {
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      }
      case message => println(message.toString)
    }
  }

  val account = system.actorOf(Props[BankAccount], "bankAccount")
  val person = system.actorOf(Props[Person], "billionaire")

  person ! LiveTheLife(account)
}
