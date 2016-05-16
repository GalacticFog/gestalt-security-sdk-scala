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
                          description: Option[String],
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

  def listGroupMemberships(creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltAccount.listGroupMemberships(id, creds)
  }
}

case object GestaltAccount {

  def listGroupMemberships(accountId: UUID, creds: Option[GestaltAPICredentials])(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"accounts/${accountId}/groups", creds)
  }

  def deregisterPhoneNumber(accountId: UUID)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.delete[GestaltAccount](s"accounts/${accountId}/phoneNumber")
  }

  def deregisterEmail(accountId: UUID)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.delete[GestaltAccount](s"accounts/${accountId}/email")
  }

  def getAccountGroups(accountId: UUID, creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"accounts/${accountId}/groups", creds)
  }

  def getAccounts(creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"accounts", creds)
  }

  def deleteAccount(accountId: UUID, creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"accounts/${accountId}", creds) map {_.wasDeleted}
  }

  def updateAccount(accountId: UUID, update: GestaltAccountUpdate, creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.patch[GestaltAccount](s"accounts/${accountId}", Json.toJson(update), creds)
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

case class GestaltGroup(id: UUID, name: String, description: Option[String], directory: GestaltDirectory, disabled: Boolean, accounts: Seq[ResourceLink]) extends GestaltResource {

  override val href: String = s"/groups/${id}"

  def listAccounts(creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] =
    GestaltGroup.listAccounts(id, creds)

  def updateMembership(add: Seq[UUID], remove: Seq[UUID], creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[ResourceLink]] = {
    GestaltGroup.updateMembership(id, add, remove, creds)
  }
}

case class GestaltGroupCreate(name: String)


case class GestaltGroupCreateWithRights(name: String,
                                        rights: Option[Seq[GestaltGrantCreate]] = None)

case object GestaltGroup {

  def listAccounts(groupId: UUID, creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"groups/${groupId}/accounts", creds)
  }

  def getGroups(creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]]("groups",creds)
  }

  def getById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"groups/${groupId}")
  }

  def deleteGroup(groupId: UUID, creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"groups/${groupId}", creds) map {_.wasDeleted}
  }

  def updateMembership(groupId: UUID, add: Seq[UUID], remove: Seq[UUID], creds: Option[GestaltAPICredentials] = None)(implicit client: GestaltSecurityClient): Future[Seq[ResourceLink]] = {
    import com.galacticfog.gestalt.io.util.PatchUpdate._
    client.patch[Seq[ResourceLink]](
      uri = s"groups/${groupId}/accounts",
      payload = Json.toJson(
        add.map {accountId => PatchOp("add","",Json.toJson(accountId))} ++
          remove.map {accountId => PatchOp("remove","",Json.toJson(accountId))}
      ),
      creds = creds
    )
  }
}
