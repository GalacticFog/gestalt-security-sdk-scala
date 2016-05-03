package com.galacticfog.gestalt.security.api

import java.util.UUID

trait GestaltToken extends GestaltResource {
  import GestaltToken._

  def tokenType: GestaltToken.TokenType
  override def href: String = tokenType match {
    case ACCESS_TOKEN => s"/accessTokens/${id}"
  }
  override def name: String = tokenType match {
    case ACCESS_TOKEN => s"accessToken-${id}"
  }
}

object GestaltToken {
  sealed trait TokenType
  final case object ACCESS_TOKEN extends TokenType
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
  gestalt_groups: Seq[GestaltGroup],
  gestalt_rights: Seq[GestaltRightGrant]) extends TokenIntrospectionResponse {
  override val active: Boolean = true
}

final case object INVALID_TOKEN extends TokenIntrospectionResponse {
  override val active: Boolean = false
}
