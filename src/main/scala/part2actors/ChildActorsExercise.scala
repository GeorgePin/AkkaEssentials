package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {

  // Distributed Word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)

    case class WordCountTask(id: Int, text: String)

    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren: Int) => {
        println("[master] initializing...")
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
      }
    }

    def withChildren(childrenRefs: IndexedSeq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String => {
        println(s"[master] I have received: $text - I will send it to child $currentChildIndex")
        val originalSender = sender
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))

      }
      case WordCountReply(id: Int, count: Int) => {
        println(s"[master] I have received a reply for task id $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
      }
    }
  }

  class WordCounterWorker extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(id: Int, text: String) => {
        println(s"${self.path} I have received a task $id with $text")
        val count = text.split(" ").length
        sender ! WordCountReply(id, count)
      }
    }
  }

  class TestActor extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case "go" => {
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I love Akka", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      }
      case count: Int => {
        println(s"[test actor] I've received a reply: $count")
      }
    }
  }

  val system = ActorSystem("roundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"
}

/*
  create WordCounterMaster
  send initialize(10) to wordCounterMaster
  send "Akka is awesome" to wordCounterMaster
    wcm will send a WordCounterTask("...") to one of its children
      child replies with a WordCountReply(3) to the master
     master replies with 3 to the sender.

  requester -> wcm -> wcw
  requester <- wcm <-
 */

// round robin logic
// 1,2,3,4,5 and 7 tasks
// 1,2,3,4,5,1,2



