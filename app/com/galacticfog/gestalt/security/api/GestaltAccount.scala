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
                          email: Option[String],
                          phoneNumber: Option[String],
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

  def generateAPICredentials(orgId: Option[UUID] = None)(implicit client: GestaltSecurityClient): Future[GestaltAPIKey] = {
    GestaltAccount.generateAPICredentials(id, orgId)
  }
}

case class GestaltAPIKey(apiKey: String,
                         apiSecret: Option[String],
                         accountId: UUID,
                         disabled: Boolean) extends GestaltResource {

  def delete()(implicit client: GestaltSecurityClient): Future[Boolean] =
    GestaltAPIKey.delete(apiKey)

  override def id: UUID = UUID.fromString(apiKey)
  override def description: Option[String] = None
  override def href: String = s"/apiKeys/${id}"
  override def name: String = s"apiKey-${id}"
}

case object GestaltAPIKey {

  def delete(apiKey: String)
            (implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"apiKeys/${apiKey}") map {_.wasDeleted}
  }

}

case object GestaltAccount {

  def generateAPICredentials(accountId: UUID, orgId: Option[UUID] = None)
                            (implicit client: GestaltSecurityClient): Future[GestaltAPIKey] = {
    client.post[GestaltAPIKey](
      s"accounts/${accountId}/apiKeys",
      orgId map {id => Json.obj(
        "orgId" -> id.toString
      )} getOrElse Json.obj()
    )
  }

  def listGroupMemberships(accountId: UUID)
                          (implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"accounts/${accountId}/groups")
  }

  def getSelf()(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.get[GestaltAccount]("accounts/self")
  }

  def deregisterPhoneNumber(accountId: UUID)
                           (implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.delete[GestaltAccount](s"accounts/${accountId}/phoneNumber")
  }

  def deregisterEmail(accountId: UUID)
                     (implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.delete[GestaltAccount](s"accounts/${accountId}/email")
  }

  def getAccountGroups(accountId: UUID)
                      (implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"accounts/${accountId}/groups")
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"accounts")
  }

  def deleteAccount(accountId: UUID)
                   (implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"accounts/${accountId}") map {_.wasDeleted}
  }

  def updateAccount(accountId: UUID, update: GestaltAccountUpdate)
                   (implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.patch[GestaltAccount](s"accounts/${accountId}", Json.toJson(update))
  }

  def getById(accountId: UUID)
             (implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"accounts/${accountId}")
  }

}

case class GestaltAccountCreate(username: String,
                                firstName: String,
                                lastName: String,
                                email: Option[String] = None,
                                phoneNumber: Option[String] = None,
                                credential: GestaltAccountCredential,
                                groups: Option[Seq[UUID]] = None,
                                description: Option[String] = None)

case class GestaltAccountUpdate(username: Option[String] = None,
                                description: Option[String] = None,
                                email: Option[String] = None,
                                phoneNumber: Option[String] = None,
                                credential: Option[GestaltAccountCredential] = None,
                                firstName: Option[String] = None,
                                lastName: Option[String] = None)

case class GestaltAccountCreateWithRights(username: String,
                                          firstName: String,
                                          lastName: String,
                                          email: Option[String] = None,
                                          phoneNumber: Option[String] = None,
                                          credential: GestaltAccountCredential,
                                          groups: Option[Seq[UUID]] = None,
                                          rights: Option[Seq[GestaltGrantCreate]] = None,
                                          description: Option[String] = None)

case class GestaltGroup(id: UUID, name: String, description: Option[String], directory: GestaltDirectory, disabled: Boolean, accounts: Seq[ResourceLink]) extends GestaltResource {

  override val href: String = s"/groups/${id}"

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] =
    GestaltGroup.listAccounts(id)

  def updateMembership(add: Seq[UUID], remove: Seq[UUID])(implicit client: GestaltSecurityClient): Future[Seq[ResourceLink]] = {
    GestaltGroup.updateMembership(id, add, remove)
  }
}

case class GestaltGroupCreate(name: String,
                              description: Option[String] = None)

case class GestaltGroupCreateWithRights(name: String,
                                        rights: Option[Seq[GestaltGrantCreate]] = None,
                                        description: Option[String] = None)

case object GestaltGroup {

  def listAccounts(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"groups/${groupId}/accounts")
  }

  def listGroups(qs: (String,String)*)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getQuery[Seq[GestaltGroup]]("groups",qs:_*)
  }

  def getById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"groups/${groupId}")
  }

  def deleteGroup(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"groups/${groupId}") map {_.wasDeleted}
  }

  def updateMembership(groupId: UUID, add: Seq[UUID], remove: Seq[UUID])(implicit client: GestaltSecurityClient): Future[Seq[ResourceLink]] = {
    import com.galacticfog.gestalt.io.util.PatchUpdate._
    client.patch[Seq[ResourceLink]](
      uri = s"groups/${groupId}/accounts",
      payload = Json.toJson(
        add.map {accountId => PatchOp("add","",Json.toJson(accountId))} ++
          remove.map {accountId => PatchOp("remove","",Json.toJson(accountId))}
      )
    )
  }
}
