package com.digileo

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.{Await, Future}
import scala.collection.immutable.List

object Cache {

  sealed trait Command

  case class Question(index: Int, replyTo: ActorRef[Answer]) extends Command

  case class LoadResponse(result: List[Value])

  def apply(): Behavior[Command] = Behaviors.setup(context => {
    val loader: ActorRef[Loader.LoadRequest] = context.spawn(Loader(), "LoaderActor")
    new CacheBehavior(context, loader)
  })

  class CacheBehavior(context: ActorContext[Command], loader: ActorRef[Loader.LoadRequest]) extends AbstractBehavior[Command](context) {
    private var values = List.empty[Value]

    private implicit val timeout = Timeout.create(context.system.settings.config.getDuration("app.routes.ask-timeout"))
    private implicit val scheduler = context.system.scheduler

    override def onMessage(message: Command): Behavior[Command] = {
      message match {
        case Question(index, replyTo) =>
          if (values.size > index) {
            replyTo ! Some(values(index))
            this
          } else {
            var shouldFetchMoreValues = true
            while (shouldFetchMoreValues) {
              val newValues = fetchMoreValues()
              values ++= newValues
              shouldFetchMoreValues = index >= values.size && newValues.size > 0
            }

            val result = if (index < values.size) Some(values(index)) else None
            replyTo ! result

            this
          }
      }
    }

    def fetchMoreValues(): List[Value] = {
      val futureLoadResponse: Future[LoadResponse] = loader.ask(Loader.LoadRequest(_))
      val response = Await.result(futureLoadResponse, timeout.duration)
      response.result
    }
  }

}

object Loader {

  sealed trait Command

  case class LoadRequest(replyTo: ActorRef[Cache.LoadResponse]) extends Command

  def apply(): Behaviors.Receive[LoadRequest] = Behaviors.receiveMessage[LoadRequest] {
    case LoadRequest(replyTo: ActorRef[Cache.LoadResponse]) =>
      val newValues = List('A')
      replyTo ! Cache.LoadResponse(newValues)
      Behaviors.same
  }
}
