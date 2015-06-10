package com.galacticfog.gestalt.security.api

case class GestaltAccount(username: String, firstName: String, lastName: String, email: String)

case class GestaltAccountCreate(username: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                credential: GestaltAccountCredential,
                                rights: Option[Seq[GestaltRightGrant]])

case class RightGrantAdd(rights: Seq[GestaltRightGrant])
case class RightGrantRemove(rightNames: Seq[String])

