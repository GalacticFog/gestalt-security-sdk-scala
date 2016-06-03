package com.galacticfog.gestalt.security.api

import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import errors._

import com.galacticfog.gestalt.security.api.json.JsonImports._

case class GestaltOrg(id: UUID, name: String, fqon: String, description: Option[String], parent: Option[ResourceLink], children: Seq[ResourceLink]) extends GestaltResource {
  override val href: String = s"/orgs/${id}"

  def createDirectory(createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[GestaltDirectory] = {
    GestaltOrg.createDirectory(id, createRequest)
  }

  def mapAccountStore(createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    GestaltOrg.mapAccountStore(id, createRequest)
  }

  def createApp(createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[GestaltApp] =
    GestaltOrg.createApp(id, createRequest)

  def createSubOrg(create: GestaltOrgCreate)(implicit client: GestaltSecurityClient): Future[GestaltOrg] =
    GestaltOrg.createSubOrg(id, create)

  def getAppByName(appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getAppByName(id, appName)
  }

  def getAccountByUsername(username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    GestaltOrg.getAccountByUsername(id, username)
  }

  def getAccountById(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    GestaltOrg.getAccountById(id, accountId)
  }

  def getGroupByName(groupname: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltOrg.getGroupByName(id, groupname)
  }

  def getGroupById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltOrg.getGroupById(id, groupId)
  }

  def getServiceApp()(implicit client: GestaltSecurityClient): Future[GestaltApp] = {
    GestaltOrg.getServiceApp(id)
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = GestaltOrg.listAccounts(id)

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = GestaltOrg.listGroups(id)

  def listGroupsByName(name: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltOrg.listGroupsByName(id, name)
  }

  def listAccountStores()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    GestaltOrg.listAccountStores(id)
  }

  @deprecated("use listDirectories","2.0.0")
  def getDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = GestaltOrg.listDirectories(id)

  def listDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] =
    GestaltOrg.listDirectories(id)

  @deprecated("use listApps","2.0.0")
  def getApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.listApps(id)

  def listApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.listApps(id)

  def listAccountGrants(username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltOrg.listAccountGrantsByUsername(id, username)
  }

  def listAccountGrants(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltOrg.listAccountGrants(id, accountId)
  }

  def listGroupGrants(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltOrg.listGroupGrants(id, groupId)
  }

  def listOrgs()(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    GestaltOrg.listOrgs(id)
  }

}

case class GestaltOrgSync(orgs: Seq[GestaltOrg], accounts: Seq[GestaltAccount], groups: Seq[GestaltGroup])

case class GestaltOrgCreate(name: String, createDefaultUserGroup: Boolean = true, description: Option[String] = None)

case object GestaltOrg {

  def listGroupGrants(orgId: UUID, groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"orgs/${orgId}/groups/${groupId}/rights")
  }

  def listAccountGrantsByUsername(orgId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"orgs/${orgId}/usernames/${username}/rights")
  }

  def listAccountGrants(orgId: UUID, accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"orgs/${orgId}/accounts/${accountId}/rights")
  }

  def listAccountStores(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    client.get[Seq[GestaltAccountStoreMapping]](s"orgs/${orgId}/accountStores")
  }

  def getGroupByName(orgId: UUID, name: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"orgs/${orgId}/groupnames/${name}")
  }

  def getGroupById(orgId: UUID, groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"orgs/${orgId}/groups/${groupId}")
  }

  def getAccountByUsername(orgId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"orgs/${orgId}/usernames/${username}")
  }

  def getAccountById(orgId: UUID, accountId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"orgs/${orgId}/accounts/${accountId}")
  }

  def getServiceApp(orgId: UUID)(implicit client: GestaltSecurityClient): Future[GestaltApp] = {
    client.get[GestaltApp](s"orgs/${orgId}/serviceApp")
  }

  def syncOrgTree(orgId: Option[UUID])(implicit client: GestaltSecurityClient): Future[GestaltOrgSync] = {
    client.get[GestaltOrgSync](
      uri = orgId map{id => s"orgs/${id}/sync"} getOrElse "sync"
    )
  }

  def createGroup(orgId: UUID, createRequest: GestaltGroupCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    client.post[GestaltGroup](s"orgs/${orgId}/groups", Json.toJson(createRequest))
  }

  def getOrgAccounts(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"orgs/${orgId}/accounts")
  }

  def createAccount(orgId: UUID, createRequest: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"orgs/${orgId}/accounts", Json.toJson(createRequest))
  }

  def mapAccountStore(orgId: UUID, createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    client.post[GestaltAccountStoreMapping](s"orgs/${orgId}/accountStores",Json.toJson(createRequest))
  }

  private[this] def noneWithLog(msg: String): PartialFunction[Throwable,Option[Nothing]] = {
    case e: Throwable =>
      Logger.info(msg, e)
      None
  }

  def authorizeFrameworkUser(creds: GestaltAPICredentials)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.withCreds(creds).postEmpty[GestaltAuthResponse](s"auth")
      .map(Option.apply)
      .recover(noneWithLog(s"failure authorizing framework user with api keys"))
  }

  def authorizeFrameworkUser(orgFQON: String, creds: GestaltAPICredentials)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.withCreds(creds).postEmpty[GestaltAuthResponse](s"${orgFQON}/auth")
      .map(Option.apply)
      .recover(noneWithLog(s"failure authorizing framework user against org ${orgFQON}"))
  }

  def authorizeFrameworkUser(orgId: UUID, creds: GestaltAPICredentials)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.withCreds(creds).postEmpty[GestaltAuthResponse](s"orgs/${orgId}/auth")
      .map(Option.apply)
      .recover(noneWithLog(s"failure authorizing framework user against org ${orgId}"))
  }

  def getAppByName(orgId: UUID, appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.listApps(orgId)
      .map { _.find {_.name == appName} }
  }

  def createDirectory(orgId: UUID, createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[GestaltDirectory] = {
    client.post[GestaltDirectory](s"orgs/${orgId}/directories",Json.toJson(createRequest))
  }

  def createSubOrg(parentOrgId: UUID, create: GestaltOrgCreate)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.post[GestaltOrg](s"orgs/${parentOrgId}/orgs",Json.toJson(create))
  }

  def deleteOrg(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"orgs/${orgId}") map {_.wasDeleted}
  }

  def createApp(orgId: UUID, createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[GestaltApp] = {
    client.post[GestaltApp](s"orgs/${orgId}/apps",Json.toJson(createRequest))
  }

  def listOrgs(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    client.get[Seq[GestaltOrg]](s"orgs/${orgId}/orgs")
  }

  def listOrgs()(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    client.get[Seq[GestaltOrg]]("orgs")
  }

  def listApps(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get[Seq[GestaltApp]](s"orgs/${orgId}/apps")
  }

  def getCurrentOrg()(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get[GestaltOrg]("orgs/current")
  }

  def listDirectories(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.get[Seq[GestaltDirectory]](s"orgs/${orgId}/directories")
  }

  def listAccounts(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"orgs/${orgId}/accounts")
  }

  def listGroups(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"orgs/${orgId}/groups")
  }

  def listGroupsByName(orgId: UUID, name: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"orgs/${orgId}/groups?name=${name}")
  }

  def getById(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOpt[GestaltOrg](s"orgs/${orgId}")
  }

  def getByFQON(fqon: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOpt[GestaltOrg](s"${fqon}")
  }

  def addGrantToAccount(orgId: UUID, accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"orgs/${orgId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(orgId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"orgs/${orgId}/groups/${groupId}/rights",Json.toJson(grant))
  }

}

