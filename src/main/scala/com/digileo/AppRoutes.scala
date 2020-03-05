package com.digileo

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.Future
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import JsonFormats._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpEncoding

class AppRoutes(cache: ActorRef[Cache.Command])(implicit val system: ActorSystem[_]) {
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getValue(index: Int): Future[Answer] = cache.ask(Cache.Question(index, _))

  val appRoutes: Route = concat(
    path("hi") {
      get {
        complete {
          "hello"
        }
      }
    },
    path(IntNumber) { index: Int =>
      get {
          val result = getValue(index)
          onSuccess(result) {
            case None => complete(StatusCodes.InternalServerError ,"")
            case Some(value) => complete(value.toString)
          }
      }
    }
  )
}
