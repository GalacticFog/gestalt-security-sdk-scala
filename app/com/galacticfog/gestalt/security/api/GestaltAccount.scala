package com.galacticfog.gestalt.security.api

import java.util.UUID
import com.galacticfog.gestalt.io.util.PatchOp
import com.galacticfog.gestalt.security.api.json.JsonImports._
import play.api.libs.json.{JsString, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class GestaltAccount(id: UUID,
                          username: String,
                          firstName: String,
                          lastName: String,
                          email: String,
                          phoneNumber: String,
                          directory: GestaltDirectory)
  extends GestaltResource
  with PatchSupport[GestaltAccount]
{
  override val name: String = username
  override val href: String = s"/accounts/${id}"

  def deregisterEmail()(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    GestaltAccount.deregisterEmail(id)
  }

  def deregisterPhoneNumber()(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    GestaltAccount.deregisterPhoneNumber(id)
  }

  def listGroupMemberships()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltAccount.listGroupMemberships(id)
  }
}

case object GestaltAccount {

  def listGroupMemberships(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"accounts/${accountId}/groups")
  }

  def deregisterPhoneNumber(accountId: UUID)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.deleteJson[GestaltAccount](s"accounts/${accountId}/phoneNumber")
  }

  def deregisterEmail(accountId: UUID)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.deleteJson[GestaltAccount](s"accounts/${accountId}/email")
  }

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

  def getById(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"accounts/${accountId}")
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

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] =
    GestaltGroup.listAccounts(id)

  def updateMembership(add: Seq[UUID] = Seq(), remove: Seq[UUID] = Seq())(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltGroup.updateMembership(id, add, remove)
  }
}

case class GestaltGroupCreate(name: String)


case class GestaltGroupCreateWithRights(name: String,
                                        rights: Option[Seq[GestaltGrantCreate]] = None)

case object GestaltGroup {

  def listAccounts(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"groups/${groupId}/accounts")
  }

  def getGroups(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getWithAuth[Seq[GestaltGroup]]("groups",username,password)
  }

  def getById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"groups/${groupId}")
  }

  def deleteGroup(groupId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"groups/${groupId}", username, password) map {_.wasDeleted}
  }

  def updateMembership(groupId: UUID, add: Seq[UUID], remove: Seq[UUID])(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    import com.galacticfog.gestalt.io.util.PatchUpdate._
    client.patchWithAuth[Seq[GestaltAccount]](s"groups/${groupId}/accounts",
    payload = Json.toJson(
      add.map {accountId => PatchOp("add","",Json.toJson(accountId))} ++
      remove.map {accountId => PatchOp("remove","",Json.toJson(accountId))}
    ),
    username = client.apiKey, password = client.apiSecret)
  }
}
