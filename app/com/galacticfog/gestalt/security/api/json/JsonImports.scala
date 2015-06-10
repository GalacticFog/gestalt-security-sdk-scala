package com.galacticfog.gestalt.security.api.json

import com.galacticfog.gestalt.security.api._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


object JsonImports {


  ///////////////////////////////////////////////////////////////////////////
  // Resources
  ///////////////////////////////////////////////////////////////////////////

  implicit val orgFormat = Json.format[GestaltOrg]
  implicit val appFormat = Json.format[GestaltApp]
  implicit val acctFormat = Json.format[GestaltAccount]
  implicit val grantFormat = Json.format[GestaltRightGrant]
  implicit val authFormat = Json.format[GestaltAuthResponse]

  implicit val basicAuthTokenFormat = Json.format[GestaltBasicCredsToken]

  val passwordCredentialFormat = Json.format[GestaltPasswordCredential]

  val credentialRead = new Reads[GestaltAccountCredential] {
    override def reads(json: JsValue): JsResult[GestaltAccountCredential] = {
      (json \ "credentialType").asOpt[String] match {
        case None => JsError("credential did not contain \"credentialType\"")
        case Some(ctype) => ctype match {
          case GestaltPasswordCredential.CREDENTIAL_TYPE => json.validate[GestaltPasswordCredential](passwordCredentialFormat)
          case _ => JsError(s"unknown credential type $ctype")
        }
      }
    }
  }

  val credentialWrite = new Writes[GestaltAccountCredential] {
    override def writes(o: GestaltAccountCredential): JsValue = {
      val js = o match {
        case p: GestaltPasswordCredential => Json.toJson(p)(passwordCredentialFormat)
      }
      js.asInstanceOf[JsObject] ++ Json.obj(
        "credentialType" -> o.credentialType
      )
    }
  }

  implicit val accountCredentialsFormat = Format(credentialRead,credentialWrite)

  implicit val accountCreateRequest = Json.format[GestaltAccountCreate]

  implicit val deleteResultFormat = Json.format[DeleteResult]

  ///////////////////////////////////////////////////////////////////////////
  // Exceptions/errors
  ///////////////////////////////////////////////////////////////////////////

  val exceptionWrites = new Writes[SecurityRESTException] {
    override def writes(o: SecurityRESTException): JsValue = {
      Json.obj(
        "code" -> o.code,
        "message" -> o.message,
        "resource" -> o.resource,
        "developerMessage" -> o.developerMessage
      )
    }
  }

  val exceptionReads = new Reads[SecurityRESTException] {
    override def reads(json: JsValue): JsResult[SecurityRESTException] = {
      (json \ "code").asOpt[Int] match {
        case Some(code) =>
          val message = (json \ "message").asOpt[String].getOrElse("")
          val devMessage = (json \ "developerMessage").asOpt[String].getOrElse("")
          val resource = (json \ "resource").asOpt[String].getOrElse("unknown")
          code match {
            case 400 => JsSuccess(BadRequestException(resource = resource, message = message, developerMessage = devMessage))
            case 401 => JsSuccess(UnauthorizedAPIException(message = message, developerMessage = devMessage))
            case 403 => JsSuccess(ForbiddenAPIException(message = message, developerMessage = devMessage))
            case 404 => JsSuccess(ResourceNotFoundException(resource = resource, message = message, developerMessage = devMessage))
            case 409 => JsSuccess(CreateConflictException(resource = resource, message = message, developerMessage = devMessage))
            case _ => JsSuccess(UnknownAPIException(code = code, resource = resource, message = message, developerMessage = devMessage))
          }
        case None => JsError("expected code: Int")
      }
    }
  }

  implicit val exceptionFormat = Format(exceptionReads,exceptionWrites)


}
