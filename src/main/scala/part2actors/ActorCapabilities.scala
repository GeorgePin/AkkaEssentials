package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

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

}
