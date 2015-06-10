package com.galacticfog.gestalt.security.api

import play.api.libs.json.JsValue

sealed abstract class SecurityRESTException(val code: Int, val resource: String, val message: String, val developerMessage: String) extends Throwable {
  override def getMessage(): String = s"code ${code}: ${message}"
}
case class UnauthorizedAPIException(override val message: String, override val developerMessage: String) extends SecurityRESTException(401,"",message,developerMessage)
case class ForbiddenAPIException(override val message: String, override val developerMessage: String) extends SecurityRESTException(403,"",message,developerMessage)
case class ResourceNotFoundException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(404,resource,message,developerMessage)
case class CreateConflictException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(409,resource,message,developerMessage)
case class BadRequestException(override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(400,resource,message,developerMessage)
case class UnknownAPIException(override val code: Int, override val resource: String, override val message: String, override val developerMessage: String) extends SecurityRESTException(code,resource,message,developerMessage)
