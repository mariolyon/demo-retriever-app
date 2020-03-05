package com.digileo

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import akka.util.Timeout

import scala.concurrent.Future

class AppRoutes(service: ActorRef[Service.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout =
    Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getItem(index: Int): Future[Answer] = service.ask(Service.Question(index, _))

  implicit def rejectionHandler = RejectionHandler.default

  val appRoutes: Route = concat(
    path(IntNumber) { index: Int =>
      get {
          val result = getItem(index)
          onSuccess(result) {
            case None => complete(StatusCodes.InternalServerError ,"")
            case Some(value) => complete(value.toString)
          }
      }
    }
  )
}
