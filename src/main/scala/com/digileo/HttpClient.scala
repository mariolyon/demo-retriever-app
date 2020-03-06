package com.digileo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.unmarshalling.Unmarshal

trait HttpClient {
  def getBodyAsString(url: String): Future[String]
}

class SimpleHttpClient(actorSystem: ActorSystem, implicit val materializer: Materializer, implicit val ec: ExecutionContext) extends  HttpClient {

  override def getBodyAsString(url: String): Future[String] = {
    val futureResponse: Future[HttpResponse] = Http()(actorSystem).singleRequest(HttpRequest(uri = url))

    futureResponse.flatMap(response => Unmarshal(response.entity).to[String])
  }
}
