package com.galacticfog.gestalt.security.api

import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import errors._

import com.galacticfog.gestalt.security.api.json.JsonImports._

case class GestaltOrg(id: UUID, name: String, fqon: String, parent: Option[ResourceLink], children: Seq[ResourceLink]) extends GestaltResource {
  override val href: String = s"/orgs/${id}"

  def createDirectory(createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[GestaltDirectory] = {
    GestaltOrg.createDirectory(id, createRequest)
  }

  def mapAccountStore(createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    GestaltOrg.mapAccountStore(id, createRequest)
  }

  def createApp(createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[GestaltApp] =
    GestaltOrg.createApp(id, createRequest)

  def createSubOrg(create: GestaltOrgCreate, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] =
    GestaltOrg.createSubOrg(id, create, username, password)

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

  def listAccountStores()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    GestaltOrg.listAccountStores(id)
  }

  @deprecated("use listDirectories","2.0.0")
  def getDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = GestaltOrg.listDirectories(id)

  def listDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] =
    GestaltOrg.listDirectories(id)

  def listDirectories(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] =
    GestaltOrg.listDirectories(id,username,password)

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

case class GestaltOrgSync(accounts: Seq[GestaltAccount], groups: Seq[GestaltGroup], orgs: Seq[GestaltOrg])

case class GestaltOrgCreate(name: String, createDefaultUserGroup: Boolean)

case class GestaltOrgUpdate(name: String)

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

  def syncOrgTree(orgId: Option[UUID], username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrgSync] = {
    client.getWithAuth[GestaltOrgSync](
      uri = orgId map{id => s"orgs/${id}/sync"} getOrElse "sync",
      username = username,
      password = password
    )
  }

  def createGroup(orgId: UUID, createRequest: GestaltGroupCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    client.post[GestaltGroup](s"orgs/${orgId}/groups", Json.toJson(createRequest))
  }

  def createGroup(orgId: UUID, createRequest: GestaltGroupCreateWithRights, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    client.postWithAuth[GestaltGroup](s"orgs/${orgId}/groups", Json.toJson(createRequest), username, password)
  }

  def getOrgAccounts(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.getWithAuth[Seq[GestaltAccount]](s"orgs/${orgId}/accounts",username,password)
  }

  def createAccount(orgId: UUID, createRequest: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"orgs/${orgId}/accounts", Json.toJson(createRequest))
  }

  def createAccount(orgId: UUID, createRequest: GestaltAccountCreateWithRights, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.postWithAuth[GestaltAccount](s"orgs/${orgId}/accounts", Json.toJson(createRequest), username, password)
  }

  def mapAccountStore(orgId: UUID, createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    client.post[GestaltAccountStoreMapping](s"orgs/${orgId}/accountStores",Json.toJson(createRequest))
  }

  private[this] def noneWithLog(msg: String): PartialFunction[Throwable,Option[Nothing]] = {
    case e: Throwable =>
      Logger.info(msg, e)
      None
  }

  def grantPasswordToken(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postForm[AccessTokenResponse](s"${orgFQON}/oauth/issue", Map(
      "grant_type" -> "password",
      "username"  -> username,
      "password"  -> password
    )) map Option.apply recover noneWithLog(s"failure retrieving password grant token from org ${orgFQON}")
  }

  def grantPasswordToken(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[AccessTokenResponse]] = {
    client.postForm[AccessTokenResponse](s"orgs/${orgId}/oauth/issue", Map(
      "grant_type" -> "password",
      "username"  -> username,
      "password"  -> password
    )) map Option.apply recover noneWithLog(s"failure retrieving password grant token from org ${orgId}")
  }

  def validateToken(orgFQON: String, token: GestaltToken)(implicit client: GestaltSecurityClient): Future[TokenIntrospectionResponse] = {
    client.postForm[TokenIntrospectionResponse](s"${orgFQON}/oauth/inspect", Map(
      "token" -> token.toString
    ))
  }

  def validateToken(orgId: UUID, token: GestaltToken)(implicit client: GestaltSecurityClient): Future[TokenIntrospectionResponse] = {
    client.postForm[TokenIntrospectionResponse](s"orgs/${orgId}/oauth/inspect", Map(
      "token" -> token.toString
    ))
  }

  def authorizeFrameworkUser(apiKey: String, apiSecret: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"auth", username = apiKey, password = apiSecret)
      .map(Option.apply)
      .recover(noneWithLog(s"failure authorizing framework user with api keys"))
  }

  def authorizeFrameworkUser(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"${orgFQON}/auth", username = username, password = password)
      .map(Option.apply)
      .recover(noneWithLog(s"failure authorizing framework user against org ${orgFQON}"))
  }

  def authorizeFrameworkUser(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"orgs/${orgId}/auth", username = username, password = password)
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
    client.post[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(create))
  }

  def createSubOrg(parentOrgId: UUID, create: GestaltOrgCreate, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.postWithAuth[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(create),username,password)
  }

  def createSubOrg(parentOrgId: UUID, name: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.post[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(name = name, createDefaultUserGroup = true)))
  }

  def createSubOrg(parentOrgId: UUID, name: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.postWithAuth[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(name = name, createDefaultUserGroup = true)),username,password)
  }

  def deleteOrg(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"orgs/${orgId}") map { _.wasDeleted }
  }

  def deleteOrg(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"orgs/${orgId}", username = username, password = password) map {_.wasDeleted}
  }

  def createApp(orgId: UUID, createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[GestaltApp] = {
    client.post[GestaltApp](s"orgs/${orgId}/apps",Json.toJson(createRequest))
  }

  def listOrgs(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    client.get[Seq[GestaltOrg]](s"orgs/${orgId}/orgs")
  }

  @deprecated("use listOrgs","2.0.0")
  def getOrgs(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    listOrgs(username, password)
  }

  def listOrgs(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
    client.getWithAuth[Seq[GestaltOrg]]("orgs",username,password)
  }

  @deprecated("use listApps","2.0.0")
  def getApps(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = listApps(orgId)

  def listApps(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get[Seq[GestaltApp]](s"orgs/${orgId}/apps")
  }

  def getCurrentOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get[GestaltOrg]("orgs/current")
  }

  def getCurrentOrg(username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.getWithAuth[GestaltOrg]("orgs/current", username, password)
  }

  @deprecated("use listDirectories","2.0.0")
  def getDirectories(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.get[Seq[GestaltDirectory]](s"orgs/${orgId}/directories")
  }

  def listDirectories(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.get[Seq[GestaltDirectory]](s"orgs/${orgId}/directories")
  }

  def listDirectories(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.getWithAuth[Seq[GestaltDirectory]](s"orgs/${orgId}/directories", username, password)
  }

  def listAccounts(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"orgs/${orgId}/accounts")
  }

  def listGroups(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"orgs/${orgId}/groups")
  }

  def getById(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOpt[GestaltOrg](s"orgs/${orgId}")
  }

  def getByFQON(fqon: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOpt[GestaltOrg](s"${fqon}")
  }

  def getById(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOptWithAuth[GestaltOrg](s"orgs/${orgId}", username = username, password = password)
  }

  def getByFQON(fqon: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    client.getOptWithAuth[GestaltOrg](s"${fqon}", username = username, password = password)
  }

  def addGrantToAccount(orgId: UUID, accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"orgs/${orgId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(orgId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"orgs/${orgId}/groups/${groupId}/rights",Json.toJson(grant))
  }

}

