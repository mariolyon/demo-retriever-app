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

  "CacheBehavior" should {
    "tell the Loader to load values if the value is not in the cache" in {
      var loadCalls:Int = 0
      val mockLoader = testKit.spawn(Behaviors.receiveMessage[Loader.LoadRequest] {
        case Loader.LoadRequest(replyTo: ActorRef[Cache.LoadResponse]) => {
          loadCalls += 1
          replyTo ! Cache.LoadResponse(List.empty)
          Behaviors.same
        }
      })

      val cacheBehavior = Behaviors.setup[Cache.Command](context => {
        val loader: ActorRef[Loader.LoadRequest] = mockLoader
        new CacheBehavior(context, loader)
      })

      val cacheActor = testKit.spawn(cacheBehavior)

      val probe = testKit.createTestProbe[Answer]()
      cacheActor ! Cache.Question(0, probe.ref)
      probe.expectMessage(None)
      loadCalls should equal(1)
    }
  }
}
