package com.galacticfog.gestalt.security.api

import java.util.UUID

case class GestaltAccount(id: UUID, username: String, firstName: String, lastName: String, email: String, phoneNumber: String, directoryId: UUID) extends GestaltResource {
  override val name: String = username
  override val href: String = s"/accounts/${id}"
}

case class GestaltAccountCreate(username: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                phoneNumber: String,
                                credential: GestaltAccountCredential)

case class GestaltAccountCreateWithRights(username: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                phoneNumber: String,
                                credential: GestaltAccountCredential,
                                rights: Option[Seq[GestaltRightGrant]])

case class GestaltGroup(id: UUID, name: String, directoryId: UUID, disabled: Boolean) extends GestaltResource {
  override val href: String = s"/groups/${id}"
}
