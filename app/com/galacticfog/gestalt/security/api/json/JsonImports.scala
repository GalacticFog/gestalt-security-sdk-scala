package com.galacticfog.gestalt.security.api.json

import java.util.UUID

import com.galacticfog.gestalt.security.api.AccessTokenResponse.BEARER
import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import com.galacticfog.gestalt.security.api._
import com.galacticfog.gestalt.security.api.errors._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.util.Try


object JsonImports {

  ///////////////////////////////////////////////////////////////////////////
  // Resources
  ///////////////////////////////////////////////////////////////////////////

  val directoryTypeReads = new Reads[DirectoryType] {
    override def reads(json: JsValue): JsResult[DirectoryType] = json match {
      case JsString(v) if v.toUpperCase == "INTERNAL" => JsSuccess(DIRECTORY_TYPE_INTERNAL)
      case JsString(v) if v.toUpperCase == "LDAP" => JsSuccess(DIRECTORY_TYPE_LDAP)
      case _ => JsError("invalid DirectoryType")
    }
  }

  val directoryTypeWrites = new Writes[DirectoryType] {
    override def writes(o: DirectoryType): JsValue = JsString(o.label)
  }

  implicit val directoryTypeFormat = Format[DirectoryType](directoryTypeReads, directoryTypeWrites)


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
  implicit val appFormat = Json.format[GestaltApp]
  implicit val dirFormat = Json.format[GestaltDirectory]
  implicit val groupFormat = Json.format[GestaltGroup]
  implicit val acctFormat = Json.format[GestaltAccount]
  implicit val grantFormat = Json.format[GestaltRightGrant]
  implicit val authFormat = Json.format[GestaltAuthResponse]
  implicit val storeMappingFormat = Json.format[GestaltAccountStoreMapping]
  implicit val mapusuReads = new Format[Map[UUID,Seq[UUID]]] {
    override def reads(json: JsValue): JsResult[Map[UUID, Seq[UUID]]] = json.validate[Map[String,Seq[String]]] flatMap {
      stringMap => Try {
        JsSuccess(stringMap.map {
          case (groupID, accountIDs) => (UUID.fromString(groupID), accountIDs.map(UUID.fromString))
        })
      } getOrElse(JsError("could not parse UUIDs in response"))
    }

    override def writes(m: Map[UUID, Seq[UUID]]): JsValue = Json.toJson(
      m.map { case (groupId, accountIDs) => (groupId.toString, accountIDs.map(_.toString))}
    )
  }
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
  implicit val accountUpdateRequest = Json.format[GestaltAccountUpdate]
  implicit val accountCreateRequestWithRights = Json.format[GestaltAccountCreateWithRights]
  implicit val groupCreateRequest = Json.format[GestaltGroupCreate]
  implicit val groupCreateRequestWithRights = Json.format[GestaltGroupCreateWithRights]
  implicit val orgAccountStoreMappingCreateRequest = Json.format[GestaltAccountStoreMappingCreate]
  implicit val appCreateRequest = Json.format[GestaltAppCreate]
  implicit val orgCreateRequestWrite = Json.writes[GestaltOrgCreate]
  val orgCreateRequestReadV2 = Json.reads[GestaltOrgCreate]
  implicit val orgCreateRequestReads = new Reads[GestaltOrgCreate] {
    override def reads(json: JsValue): JsResult[GestaltOrgCreate] = {
      lazy val v1 = (json \ "orgName").asOpt[String] map {n => GestaltOrgCreate(name = n, createDefaultUserGroup = false)}
      json.asOpt[GestaltOrgCreate](orgCreateRequestReadV2) orElse v1 match {
        case Some(c) => JsSuccess(c)
        case None => JsError("could not parse GestaltOrgCreate")
      }
    }
  }
  implicit val dirCreateRequest = Json.format[GestaltDirectoryCreate]

  val tokenTypeReads = new Reads[AccessTokenResponse.TokenType] {
    override def reads(json: JsValue): JsResult[AccessTokenResponse.TokenType] = json match {
      case JsString(v) if v.toUpperCase == BEARER.toString().toUpperCase() => JsSuccess(BEARER)
      case _ => JsError("invalid TokenType")
    }
  }
  val tokenTypeWrites = new Writes[AccessTokenResponse.TokenType] {
    override def writes(o: AccessTokenResponse.TokenType): JsValue = JsString(o.toString)
  }
  implicit val tokenTypeFormat = Format[AccessTokenResponse.TokenType](tokenTypeReads,tokenTypeWrites)
  implicit val gestaltToken = new Format[GestaltToken] {
    override def reads(json: JsValue): JsResult[GestaltToken] = json match {
      case JsString(str) => Try {
        UUID.fromString(str)
      } map {uuid => JsSuccess(OpaqueToken(uuid,ACCESS_TOKEN))} getOrElse JsError("Expecting UUID when parsing GestaltToken")
      case _ => JsError("Token format currently limited to OpaqueToken objects encoded a UUID in a JSON string")
    }
    override def writes(t: GestaltToken): JsValue = t match {
      case o: OpaqueToken => JsString(o.id.toString)
    }
  }
  val accessTokenRespReads: Reads[AccessTokenResponse] = (
    (__ \ "access_token").read[GestaltToken] and
      (__ \ "refresh_token").readNullable[GestaltToken] and
      (__ \ "token_type").read[AccessTokenResponse.TokenType] and
      (__ \ "expires_in").read[Long] and
      (__ \ "gestalt_access_token_href").read[String]
    )(AccessTokenResponse.apply _)
  val accessTokenRespWrites: Writes[AccessTokenResponse] = (
    (__ \ "access_token").write[GestaltToken] and
      (__ \ "refresh_token").writeNullable[GestaltToken] and
      (__ \ "token_type").write[AccessTokenResponse.TokenType] and
      (__ \ "expires_in").write[Long] and
      (__ \ "gestalt_access_token_href").write[String]
    )(unlift(AccessTokenResponse.unapply))
  implicit val accessTokenResp = Format(accessTokenRespReads,accessTokenRespWrites)

  val validTokenReadBase = Json.reads[ValidTokenResponse]
  val validTokenWriteBase = Json.writes[ValidTokenResponse]
  implicit val validTokenIntrospectionReads = (__ \ "active").read[Boolean](true) andKeep __.read[ValidTokenResponse](validTokenReadBase)
  val tokenIntrospectionResponseReads = new Reads[TokenIntrospectionResponse] {
    override def reads(json: JsValue): JsResult[TokenIntrospectionResponse] = {
      (json \ "active").asOpt[Boolean] match {
        case Some(true) => json.validate[ValidTokenResponse]
        case Some(false) => JsSuccess(INVALID_TOKEN)
        case None => JsError("missing field 'active'; not a valid token introspection response")
      }
    }
  }
  val tokenIntrospectionResponseWrites = new Writes[TokenIntrospectionResponse] {
    override def writes(r: TokenIntrospectionResponse): JsValue = {
      Json.obj("active" -> r.active) ++ (r match {
        case INVALID_TOKEN => Json.obj()
        case v: ValidTokenResponse => Json.toJson(v)(validTokenWriteBase).as[JsObject]
      })
    }
  }
  implicit val tokenIntrospectionResponse = Format(tokenIntrospectionResponseReads,tokenIntrospectionResponseWrites)

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
            case 401 => JsSuccess(UnauthorizedAPIException(resource = resource, message = message, developerMessage = devMessage))
            case 403 => JsSuccess(ForbiddenAPIException(message = message, developerMessage = devMessage))
            case 404 => JsSuccess(ResourceNotFoundException(resource = resource, message = message, developerMessage = devMessage))
            case 409 => JsSuccess(ConflictException(resource = resource, message = message, developerMessage = devMessage))
            case _   => JsSuccess(UnknownAPIException(code = code, resource = resource, message = message, developerMessage = devMessage))
          }
        case None => JsError("expected code: Int")
      }
    }
  }

  implicit val exceptionFormat = Format(exceptionReads,exceptionWrites)

}
