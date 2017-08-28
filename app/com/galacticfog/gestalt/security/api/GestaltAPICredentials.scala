package com.galacticfog.gestalt.security.api

import java.util.Base64

import scala.util.Try

sealed trait GestaltAPICredentials {
  def headerValue: String
}
final case class GestaltBearerCredentials(token: String) extends GestaltAPICredentials {
  override def headerValue: String = "Bearer " + token
}
final case class GestaltBasicCredentials(username: String, password: String) extends GestaltAPICredentials {
  override def headerValue: String = "Basic " + Base64.getEncoder.encodeToString( (username + ":" + password).getBytes )
}

object GestaltAPICredentials {

  private def decode(b64: String): Option[String] = Try {
    new String(Base64.getDecoder.decode(b64))
  } toOption

  def getCredentials(authHeader: String): Option[GestaltAPICredentials] = {
    authHeader.split(" ").slice(0,2) match {
      case Array("Basic", token) => for {
        decode <- decode(token)
        arr = decode.split(":")
        cred <- arr match {
          case Array(username,password) => Some(GestaltBasicCredentials(username, password))
          case _ => None
        }
      } yield cred
      case Array("Bearer", token) =>
        Some(GestaltBearerCredentials(token))
      case Array(s) if s.startsWith("token=") =>
        Some(GestaltBearerCredentials(s.stripPrefix("token=")))
      case _ => None
    }
  }

}
