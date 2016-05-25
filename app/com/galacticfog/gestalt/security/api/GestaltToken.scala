package com.galacticfog.gestalt.security.api

import java.util.UUID
import play.api.Logger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.galacticfog.gestalt.security.api.json.JsonImports._

trait GestaltToken extends GestaltResource {
  import GestaltToken._

  def tokenType: GestaltToken.TokenType
  override def href: String = tokenType match {
    case ACCESS_TOKEN => s"/accessTokens/${id}"
  }
  override def name: String = tokenType match {
    case ACCESS_TOKEN => s"accessToken-${id}"
  }
  override def description = None
}

object GestaltToken {
  sealed trait TokenType
  final case object ACCESS_TOKEN extends TokenType

  private[this] def noneWithLog(msg: String): PartialFunction[Throwable,Option[Nothing]] = {
    case e: Throwable =>
      Logger.info(msg, e)
      None
  }

  /**
    * Generate an oauth2 access token using client_credential grant workflow
    *
    * @param client SDK client bound with the appropriate client credentials
    * @return AccessTokenResponse, or None on failure
    */
  def grantClientToken()(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postForm[AccessTokenResponse](s"oauth/issue", Map(
      "grant_type" -> "client_credentials"
    )) map Option.apply recover noneWithLog(s"failure retrieving client credentials grant token against global oauth endpoint")
  }

  /**
    * Generate an oauth2 access token using client_credential grant workflow
    *
    * @param orgId Org against which to bind this token
    * @param client SDK client bound with the appropriate client credentials
    * @return AccessTokenResponse, or None on failure
    */
  def grantClientToken(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postForm[AccessTokenResponse](s"orgs/${orgId}/oauth/issue", Map(
      "grant_type" -> "client_credentials"
    )) map Option.apply recover noneWithLog(s"failure retrieving client credentials grant token from org ${orgId}")
  }

  /**
    * Generate an oauth2 access token using client_credential grant workflow
    *
    * @param orgFQON Org against which to bind this token
    * @param client SDK client bound with the appropriate client credentials
    * @return AccessTokenResponse, or None on failure
    */
  def grantClientToken(orgFQON: String)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postForm[AccessTokenResponse](s"${orgFQON}/oauth/issue", Map(
      "grant_type" -> "client_credentials"
    )) map Option.apply recover noneWithLog(s"failure retrieving client credentials grant token from org ${orgFQON}")
  }

  /**
    * Generate an oauth2 access token using resource owner password grant workflow
    *
    * @param orgFQON Org against which to authenticate these credentials and bind the token
    * @param username Resource owner username
    * @param password Resource owner password
    * @param client SDK client
    * @return AccessTokenResponse, or None on failure
    */
  def grantPasswordToken(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postFormNoAuth[AccessTokenResponse](s"${orgFQON}/oauth/issue", Map(
      "grant_type" -> "password",
      "username"  -> username,
      "password"  -> password
    )) map Option.apply recover noneWithLog(s"failure retrieving password grant token from org ${orgFQON}")
  }

  /**
    * Generate an oauth2 access token using resoruce owner password grant workflow
    *
    * @param orgId Org against which to authenticate these credentials and bind the token
    * @param username Resource owner username
    * @param password Resource owner password
    * @param client SDK client
    * @return AccessTokenResponse, or None on failure
    */
  def grantPasswordToken(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postFormNoAuth[AccessTokenResponse](s"orgs/${orgId}/oauth/issue", Map(
      "grant_type" -> "password",
      "username"  -> username,
      "password"  -> password
    )) map Option.apply recover noneWithLog(s"failure retrieving password grant token from org ${orgId}")
  }

  def validateToken(orgFQON: String, token: GestaltToken)(implicit client: GestaltSecurityClient): Future[TokenIntrospectionResponse] = {
    client.postForm[TokenIntrospectionResponse](s"${orgFQON}/oauth/inspect", Map(
      "token" -> token.toString
    ))
  }

  def validateToken(orgId: UUID, token: GestaltToken)(implicit client: GestaltSecurityClient): Future[TokenIntrospectionResponse] = {
    client.postForm[TokenIntrospectionResponse](s"orgs/${orgId}/oauth/inspect", Map(
      "token" -> token.toString
    ))
  }

  def validateToken(token: GestaltToken)(implicit client: GestaltSecurityClient): Future[TokenIntrospectionResponse] = {
    client.postForm[TokenIntrospectionResponse](s"oauth/inspect", Map(
      "token" -> token.toString
    ))
  }


}

case class OpaqueToken(override val id: UUID, override val tokenType: GestaltToken.TokenType) extends GestaltToken {
  override def toString: String = id.toString
}

case class AccessTokenResponse(accessToken: GestaltToken,
                               refreshToken: Option[GestaltToken],
                               tokenType: AccessTokenResponse.TokenType,
                               expiresIn: Long,
                               gestalt_access_token_href: String)

case object AccessTokenResponse {
  sealed trait TokenType
  final case object BEARER  extends TokenType {override def toString() = "bearer"}
}

sealed trait TokenIntrospectionResponse {
  def active: Boolean
}

final case class ValidTokenResponse(
  username: String,
  sub: String,
  iss: String,
  exp: Long,
  iat: Long,
  jti: UUID,
  gestalt_org_id: UUID,
  gestalt_token_href: String,
  gestalt_account: GestaltAccount,
  gestalt_groups: Seq[ResourceLink],
  gestalt_rights: Seq[GestaltRightGrant]) extends TokenIntrospectionResponse {
  override val active: Boolean = true
}

final case object INVALID_TOKEN extends TokenIntrospectionResponse {
  override val active: Boolean = false
}
