package com.galacticfog.gestalt.security.api

import java.util.UUID
import com.galacticfog.gestalt.security.api.json.JsonImports._
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

case class GestaltAccount(id: UUID, username: String, firstName: String, lastName: String, email: String, phoneNumber: String, directory: GestaltDirectory) extends GestaltResource {
  override val name: String = username
  override val href: String = s"/accounts/${id}"
}

case object GestaltAccount {
  def getAccountGroups(accountId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getWithAuth[Seq[GestaltGroup]](s"accounts/${accountId}/groups",username,password)
  }

  def getAccounts(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.getWithAuth[Seq[GestaltAccount]](s"accounts",username,password)
  }

  def deleteAccount(accountId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"accounts/${accountId}", username, password) map {_.wasDeleted}
  }

  def updateAccount(accountId: UUID, update: GestaltAccountUpdate, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.patchWithAuth[GestaltAccount](s"accounts/${accountId}", Json.toJson(update), username, password)
  }
}

case class GestaltAccountCreate(username: String,
                                firstName: String,
                                lastName: String,
                                email: String,
                                phoneNumber: String,
                                groups: Option[Seq[UUID]] = None,
                                credential: GestaltAccountCredential)

case class GestaltAccountUpdate(username: Option[String],
                                email: Option[String],
                                phoneNumber: Option[String],
                                credential: Option[GestaltAccountCredential],
                                firstName: Option[String],
                                lastName: Option[String])

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
                                        rights: Option[Seq[GestaltGrantCreate]] = None)

case object GestaltGroup {
  def getGroups(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getWithAuth[Seq[GestaltGroup]]("groups",username,password)
  }
}
