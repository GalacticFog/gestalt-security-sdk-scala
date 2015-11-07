package com.galacticfog.gestalt.security.api

import play.api.libs.json.{JsValue, JsResultException}

import scala.concurrent.Future
import scala.util.{Failure, Try}

package object errors {

  def handleParsingErrorAsFailure[U <: GestaltResource](implicit json: JsValue, t: reflect.Manifest[U]): PartialFunction[Throwable, Try[U]] = {
    case e: Throwable => Failure(convertParsingError(e))
  }

  def convertParsingError[U <: GestaltResource](e: Throwable)(implicit json: JsValue, t: reflect.Manifest[U]): Throwable = {
    e match {
      case js: JsResultException => APIParseException(
        resource = "unknown",
        message = "Error parsing " + t.toString() + " from successful API response",
        devMessage = "Error parsing a successful API response. Likely culprit is a version mismatch between the client and the API. Please contact the developers.",
        json = json
      )
      case e: Throwable => UnknownAPIException(
        code = 0,
        resource = "unknown",
        message = e.getMessage,
        developerMessage = "Received exception: " + e.getMessage + ", likely while parsing " + t.toString() + " from JSON response: " + json.toString()
      )
    }
  }

  def convertToFailure[U]: PartialFunction[Throwable,Try[U]] = {
    case e: Throwable => Failure(e)
  }

}
