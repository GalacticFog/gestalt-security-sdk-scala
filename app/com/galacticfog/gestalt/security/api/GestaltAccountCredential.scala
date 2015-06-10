package com.galacticfog.gestalt.security.api

sealed trait GestaltAccountCredential {
  def credentialType: String
}

case class GestaltPasswordCredential(password: String) extends GestaltAccountCredential {
  override def credentialType: String = GestaltPasswordCredential.CREDENTIAL_TYPE
}
case object GestaltPasswordCredential {
  val CREDENTIAL_TYPE: String = "password"
}
