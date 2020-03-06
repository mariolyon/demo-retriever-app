package com.digileo

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.digileo.Service.LoadResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Future

class LoaderBehaviorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with ScalaFutures {

  "Loader" should {
    "parse a payload with multiple values" in {

      val mockHttpClient = new HttpClient {
        override def getBodyAsString(url: String): Future[String] = Future.successful("A\nB\nC")
      }

      val loaderActor = testKit.spawn(Loader("http://somewhere.com", mockHttpClient))

      val probe = testKit.createTestProbe[Service.LoadResponse]()
      loaderActor ! Loader.LoadRequest(probe.ref)
      probe.expectMessage(LoadResponse(List('A', 'B', 'C')))
    }

    "parse an empty payload" in {
      val mockHttpClient = new HttpClient {
        override def getBodyAsString(url: String): Future[String] = Future.successful("")
      }

      val loaderActor = testKit.spawn(Loader("http://somewhere.com", mockHttpClient))

      val probe = testKit.createTestProbe[Service.LoadResponse]()
      loaderActor ! Loader.LoadRequest(probe.ref)
      probe.expectMessage(LoadResponse(List()))
    }

    "reply to guardian with empty list on failed request through HttpClient" in {
      val mockHttpClient = new HttpClient {
        override def getBodyAsString(url: String): Future[String] = Future.failed(new Exception("disaster struck"))
      }

      val loaderActor = testKit.spawn(Loader("http://somewhere.com", mockHttpClient))
      val probe = testKit.createTestProbe[Service.LoadResponse]()
      loaderActor ! Loader.LoadRequest(probe.ref)
      probe.expectMessage(LoadResponse(List()))
    }
  }
}
