package com.galacticfog.gestalt.security.api

import java.util.Base64

sealed trait GestaltAPICredentials {
  def headerValue: String
}
final case class GestaltBearerCredentials(token: String) extends GestaltAPICredentials {
  override def headerValue: String = token
}
final case class GestaltBasicCredentials(username: String, password: String) extends GestaltAPICredentials {
  override def headerValue: String = Base64.getEncoder.encodeToString( (username + ":" + password).getBytes )
}
