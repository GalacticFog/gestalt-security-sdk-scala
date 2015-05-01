package com.galacticfog.gestalt.security.api.json

import com.galacticfog.gestalt.security.api._
import play.api.libs.json.Json

object JsonImports {
  implicit val orgFormat = Json.format[GestaltOrg]
  implicit val appFormat = Json.format[GestaltApp]
  implicit val acctFormat = Json.format[GestaltAccount]
  implicit val grantFormat = Json.format[GestaltRightGrant]
  implicit val authFormat = Json.format[GestaltAuthResponse]

  implicit val basicAuthTokenFormat = Json.format[GestaltBasicCredsToken]
}
