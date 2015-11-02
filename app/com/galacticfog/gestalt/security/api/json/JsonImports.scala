package com.galacticfog.gestalt.security.api.json

import java.util.UUID

import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


object JsonImports {

  ///////////////////////////////////////////////////////////////////////////
  // Resources
  ///////////////////////////////////////////////////////////////////////////

  val storeTypeReads = new Reads[GestaltAccountStoreType] {
    override def reads(json: JsValue): JsResult[GestaltAccountStoreType] = json match {
      case JsString(v) if v.toUpperCase == DIRECTORY.label => JsSuccess(DIRECTORY)
      case JsString(v) if v.toUpperCase == GROUP.label     => JsSuccess(GROUP)
      case _ => JsError("invalid GestaltAccountStoreType")
    }
  }
  val storeTypeWrites = new Writes[GestaltAccountStoreType] {
    override def writes(o: GestaltAccountStoreType): JsValue = JsString(o.label)
  }
  implicit val storeTypeFormat = Format[GestaltAccountStoreType](storeTypeReads,storeTypeWrites)

  implicit val linkFormat = Json.format[ResourceLink]
  implicit val orgFormat = Json.format[GestaltOrg]
  implicit val orgTreeFormat = Json.format[GestaltOrgWithChildren]
  implicit val appFormat = Json.format[GestaltApp]
  implicit val dirFormat = Json.format[GestaltDirectory]
  implicit val groupFormat = Json.format[GestaltGroup]
  implicit val acctFormat = Json.format[GestaltAccount]
  implicit val orgAcctFormat = Json.format[GestaltOrgAccount]
  implicit val grantFormat = Json.format[GestaltRightGrant]
  implicit val authFormat = Json.format[GestaltAuthResponse]
  implicit val storeMappingFormat = Json.format[GestaltAccountStoreMapping]
  implicit val syncFormat = Json.format[GestaltOrgSync]

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

  implicit val rightCreateRequest = Json.format[GestaltGrantCreate]
  implicit val accountCreateRequest = Json.format[GestaltAccountCreate]
  implicit val accountCreateRequestWithRights = Json.format[GestaltAccountCreateWithRights]
  implicit val groupCreateRequest = Json.format[GestaltGroupCreate]
  implicit val groupCreateRequestWithRights = Json.format[GestaltGroupCreateWithRights]
  implicit val accountStoreMappingCreateRequest = Json.format[GestaltAccountStoreMappingCreate]
  implicit val accountStoreMappingUpdateRequest = Json.format[GestaltAccountStoreMappingUpdate]
  implicit val appCreateRequest = Json.format[GestaltAppCreate]
  implicit val orgCreateRequest = Json.format[GestaltOrgCreate]
  implicit val dirCreateRequest = Json.format[GestaltDirectoryCreate]

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
            case _   => JsSuccess(UnknownAPIException(code = code, resource = resource, message = message, developerMessage = devMessage))
          }
        case None => JsError("expected code: Int")
      }
    }
  }

  implicit val exceptionFormat = Format(exceptionReads,exceptionWrites)

}
