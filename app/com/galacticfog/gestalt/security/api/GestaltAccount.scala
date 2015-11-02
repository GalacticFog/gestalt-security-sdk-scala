package com.galacticfog.gestalt.security.api

import java.util.UUID
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.concurrent.Future
import scala.util.Try

case class GestaltAccount(id: UUID, username: String, firstName: String, lastName: String, email: String, phoneNumber: String, directory: GestaltDirectory) extends GestaltResource {
  override val name: String = username
  override val href: String = s"/accounts/${id}"
}

case object GestaltAccount {
  def getAccountGroups(accountId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltGroup]]] = {
    client.getTryWithAuth[Seq[GestaltGroup]](s"accounts/${accountId}/groups",username,password)
  }

  def getAccounts(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltAccount]]] = {
    client.getTryWithAuth[Seq[GestaltAccount]](s"accounts",username,password)
  }
}

case class GestaltAccountCreate(username: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                phoneNumber: String,
                                groups: Option[Seq[UUID]] = None,
                                credential: GestaltAccountCredential)

case class GestaltAccountCreateWithRights(username: String,
                                          firstName: String,
                                          lastName: String,
                                          email: String,
                                          phoneNumber: String,
                                          credential: GestaltAccountCredential,
                                          groups: Option[Seq[UUID]] = None,
                                          rights: Option[Seq[GestaltGrantCreate]] = None)

case class GestaltGroup(id: UUID, name: String, directoryId: UUID, disabled: Boolean) extends GestaltResource {
  override val href: String = s"/groups/${id}"
}

case class GestaltGroupCreate(name: String)


case class GestaltGroupCreateWithRights(name: String,
                                        rights: Option[Seq[GestaltRightGrant]] = None)

case object GestaltGroup {
  def getGroups(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltGroup]]] = {
    client.getTryWithAuth[Seq[GestaltGroup]]("groups",username,password)
  }
}
