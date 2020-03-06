package com.digileo.actor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.util.Timeout
import com.digileo.util.{Cache, SimpleHttpClient}
import com.digileo.{Answer, Value}

import scala.collection.immutable.List
import scala.concurrent.{Await, Future}

object Service {

  sealed trait Command

  case class Question(index: Int, replyTo: ActorRef[Answer]) extends Command

  case class LoadResponse(result: List[Value])

  def apply(): Behavior[Command] = Behaviors.setup(context => {
    val sourceUrl = context.system.settings.config.getString("app.source-url")

    val httpClient = {
      val actorSystem = akka.actor.ActorSystem()
      val materializer = Materializer(context)
      val ec = context.system.executionContext
      new SimpleHttpClient(actorSystem, materializer, ec)
    }

    val loader: ActorRef[Loader.LoadRequest] = context.spawn(Loader(sourceUrl, httpClient), "LoaderActor")
    new ServiceBehavior(context, loader)
  })

  class ServiceBehavior(context: ActorContext[Command], loader: ActorRef[Loader.LoadRequest]) extends AbstractBehavior[Command](context) {
    private val cache = new Cache()

    private implicit val timeout = Timeout.create(context.system.settings.config.getDuration("app.routes.ask-timeout"))
    private implicit val scheduler = context.system.scheduler

    override def onMessage(message: Command): Behavior[Command] = open(message)

    private def open(message: Command) = message match {
      case Question(index, replyTo) =>
        cache.get(index) match {
          case answer@Some(_) => {
            replyTo ! answer
            Behaviors.same[Command]
          }
          case None => {
            var shouldFetchMoreValues = true
            while (shouldFetchMoreValues) {
              val newValues = fetchMoreValues()
              cache.add(newValues)
              shouldFetchMoreValues = cache.contains(index) == false && newValues.size > 0
            }
            replyTo ! cache.get(index)
            Behaviors.same[Command]
          }
        }
    }

    private def openx: Behavior[Command] = Behaviors.receiveMessage[Command](open)




    def fetchMoreValues(): List[Value] = {
      val futureLoadResponse: Future[LoadResponse] = loader.ask(Loader.LoadRequest(_))
      val response = Await.result(futureLoadResponse, timeout.duration)
      response.result
    }



  }

}

