package com.digileo

import spray.json.DefaultJsonProtocol

object JsonFormats  {
  import DefaultJsonProtocol._

  implicit val optionalValueJsonFormat = jsonFormat1(Option[Value])

}
