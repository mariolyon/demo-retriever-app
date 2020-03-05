package com.digileo

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import com.digileo.Cache.CacheBehavior
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

class CacheBehaviorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with ScalaFutures {
  var lastResult: Option[Value] = None

  val mockGuardian = testKit.spawn(Behaviors.receiveMessage[Answer] {
    case result: Option[Value] => {
      lastResult = result
      Behaviors.same
    }
  })

  "Cache" should {
    "call Loader enough times to be able to retrieve a queried item" in {
      var loadCalls:Int = 0
      val mockLoader = testKit.spawn(Behaviors.receiveMessage[Loader.LoadRequest] {
        case Loader.LoadRequest(replyTo: ActorRef[Cache.LoadResponse]) => {
          val result = if(loadCalls == 0) {
            List('A', 'B')
          } else if(loadCalls == 1) {
            List('C', 'D', 'E')
          } else {
            List()
          }

          loadCalls += 1
          replyTo ! Cache.LoadResponse(result)

          Behaviors.same
        }
      })

      val cacheBehavior = Behaviors.setup[Cache.Command](context => {
        val loader: ActorRef[Loader.LoadRequest] = mockLoader
        new CacheBehavior(context, loader)
      })

      val cacheActor = testKit.spawn(cacheBehavior)

      val probe = testKit.createTestProbe[Answer]()
      cacheActor ! Cache.Question(4, probe.ref)
      probe.expectMessage(Some('E'))
      loadCalls should equal(2)

      // Getting an item already in the cache should not trigger a call to Loader.
      cacheActor ! Cache.Question(0, probe.ref)
      probe.expectMessage(Some('A'))
      loadCalls should equal(2)

      // If an unknown item is requested and the Load call returns no data the answer
      // should be None, and only 1 additional call to Loader should be made;
      // In other words we should not get an infinite loop.
      cacheActor ! Cache.Question(5, probe.ref)
      probe.expectMessage(None)

      loadCalls should equal(3)
    }
  }
}
