package com.digileo.actor

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.util.Timeout
import com.digileo.util.{Cache, SimpleHttpClient}
import com.digileo.{Answer, Value}

import scala.collection.immutable.List

object Service {

  sealed trait Command

  case class Question(index: Int, replyTo: ActorRef[Answer]) extends Command

  case class LoadResponse(result: List[Value]) extends Command

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

    override def onMessage(message: Command): Behavior[Command] = questionHandler(message)

    private def questionHandler(message: Command): Behavior[Command] = message match {
        case Question(index, replyTo) =>
          cache.get(index) match {
            case answer@Some(_) =>
              replyTo ! answer
              Behaviors.same
            case None =>
              loader ! Loader.LoadRequest(context.self)
              waitForLoadBehavior(index, replyTo)
          }
        case _ => Behaviors.unhandled
      }

    private def loadResponseHandler(index: Int, replyTo: ActorRef[Answer]): (Command) => Behavior[Command] =
      (message: Command) => message match {
        case LoadResponse(newValues) =>
          cache.add(newValues)
          if (!cache.contains(index) && newValues.size > 0) {
            loader ! Loader.LoadRequest(context.self)
            Behaviors.same
          } else {
            replyTo ! cache.get(index)
            waitForQuestionBehavior
          }
        case _ => Behaviors.unhandled
      }

    private def waitForLoadBehavior(index: Int, replyTo: ActorRef[Answer]): Behavior[Command] = Behaviors.receiveMessage[Command](loadResponseHandler(index, replyTo))

    private def waitForQuestionBehavior: Behavior[Command] = Behaviors.receiveMessage[Command](questionHandler)

  }

}

