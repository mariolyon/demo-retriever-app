package com.digileo.actor

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import com.digileo.util.HttpClient

import scala.collection.immutable.List
import scala.concurrent.Future
import scala.util.{Failure, Success}

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
