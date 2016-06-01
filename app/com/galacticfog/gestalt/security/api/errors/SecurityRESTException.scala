package com.galacticfog.gestalt.security.api.errors

import play.api.libs.json.JsValue

sealed abstract class SecurityRESTException(val code: Int, val resource: String, val message: String, val developerMessage: String) extends Throwable {
  override def getMessage(): String = s"code ${code}: ${message}"
}

case class OAuthError(error: String, error_description: String, code: Option[Int] = Some(400)) extends Throwable {
  override def getMessage: String = error + ":" + error_description
}

case class BadRequestException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(400,resource,message,developerMessage)
case class UnauthorizedAPIException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(401,resource,message,developerMessage)
case class ForbiddenAPIException(override val message: String, override val developerMessage: String) extends SecurityRESTException(403,"",message,developerMessage)
case class ResourceNotFoundException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(404,resource,message,developerMessage)
case class ConflictException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(409,resource,message,developerMessage)
case class UnknownAPIException(override val code: Int, override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(code,resource,message,developerMessage)
case class APIParseException(override val resource: String, override val message: String, devMessage: String, json: JsValue) extends SecurityRESTException(
  code=0,
  resource=resource,
  message=message,
  developerMessage=devMessage + "\nResponse JSON: " + json.toString()
)
