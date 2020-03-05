package com.digileo

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.{Await, Future}
import scala.collection.immutable.List

object Service {

  sealed trait Command

  case class Question(index: Int, replyTo: ActorRef[Answer]) extends Command

  case class LoadResponse(result: List[Value])

  def apply(): Behavior[Command] = Behaviors.setup(context => {
    val loader: ActorRef[Loader.LoadRequest] = context.spawn(Loader(), "LoaderActor")
    new ServiceBehavior(context, loader)
  })

  class ServiceBehavior(context: ActorContext[Command], loader: ActorRef[Loader.LoadRequest]) extends AbstractBehavior[Command](context) {
    private val cache = new Cache()

    private implicit val timeout = Timeout.create(context.system.settings.config.getDuration("app.routes.ask-timeout"))
    private implicit val scheduler = context.system.scheduler

    override def onMessage(message: Command): Behavior[Command] = {
      message match {
        case Question(index, replyTo) =>
          cache.get(index) match {
            case answer@Some(_) => {
              replyTo ! answer
              this
            }
            case None => {
              var shouldFetchMoreValues = true
              while (shouldFetchMoreValues) {
                val newValues = fetchMoreValues()
                cache.add(newValues)
                shouldFetchMoreValues = cache.contains(index) == false && newValues.size > 0
              }
              replyTo ! cache.get(index)
              this
            }
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

  case class LoadRequest(replyTo: ActorRef[Service.LoadResponse]) extends Command

  def apply(): Behaviors.Receive[LoadRequest] = Behaviors.receiveMessage[LoadRequest] {
    case LoadRequest(replyTo: ActorRef[Service.LoadResponse]) =>
      val newValues = List('A')
      replyTo ! Service.LoadResponse(newValues)
      Behaviors.same
  }
}
