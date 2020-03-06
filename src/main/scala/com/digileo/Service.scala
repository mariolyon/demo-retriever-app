package com.digileo

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.Materializer
import akka.util.Timeout

import scala.collection.immutable.List
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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

  def apply(sourceUrl: String, httpClient: HttpClient): Behaviors.Receive[LoadRequest] = Behaviors.receive[LoadRequest] {
    (context, message) => message match {
      case LoadRequest(replyTo: ActorRef[Service.LoadResponse]) => {
        implicit val executionContext = context.system.executionContext

        val futurePayload: Future[String] = httpClient.getBodyAsString(sourceUrl)

        futurePayload.onComplete {
          case Success(payload) =>
            val newValues = payload.split("\n").filterNot(_.isBlank).map(_.charAt(0)).toList
            println(s"Read ${newValues.size} values.")
            replyTo ! Service.LoadResponse(newValues)
          case Failure(exception) =>
            println(s"Exception return from HttpClient: ${exception}.")
            replyTo ! Service.LoadResponse(List.empty)
        }

        Behaviors.same[LoadRequest]
      }
    }
  }

}
