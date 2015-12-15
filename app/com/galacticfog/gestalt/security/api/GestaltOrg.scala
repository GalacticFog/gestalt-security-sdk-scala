package com.galacticfog.gestalt.security.api

import java.util.UUID
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

  def getGroupByGroupnme(groupname: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltOrg.getGroupByUsername(id, groupname)
  }

  def getGroupById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltOrg.getGroupById(id, groupId)
  }

  def getServiceApp()(implicit client: GestaltSecurityClient): Future[GestaltApp] = {
    GestaltOrg.getServiceApp(id)
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = GestaltOrg.listAccounts(id)

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = GestaltOrg.listGroups(id)

  @deprecated("use listDirectories","2.0.0")
  def getDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = GestaltOrg.listDirectories(id)

  def listDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] =
    GestaltOrg.listDirectories(id)

  def listDirectories(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] =
    GestaltOrg.listDirectories(id,username,password)

  @deprecated("use listApps","2.0.0")
  def getApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.listApps(id)

  def listApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.listApps(id)
}

case class GestaltOrgSync(accounts: Seq[GestaltAccount], orgs: Seq[GestaltOrg])

case class GestaltOrgCreate(name: String, createDefaultUserGroup: Option[Boolean] = None)

case class GestaltOrgUpdate(name: String)

case object GestaltOrg {
  def getGroupById(orgId: UUID, groupId: UUID): Future[Option[GestaltGroup]] = ???


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

  def authorizeFrameworkUser(apiKey: String, apiSecret: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"auth", username = apiKey, password = apiSecret) map {Some(_)} recover {case _ => None}
  }

  def authorizeFrameworkUser(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"${orgFQON}/auth", username = username, password = password) map {Some(_)} recover {case _ => None}
  }

  def authorizeFrameworkUser(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth[GestaltAuthResponse](s"orgs/${orgId}/auth", username = username, password = password) map {Some(_)} recover {case _ => None}
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
    client.post[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(name = name)))
  }

  def createSubOrg(parentOrgId: UUID, name: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.postWithAuth[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(name = name)),username,password)
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

  def getOrgs(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltOrg]] = {
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
    client.post[GestaltRightGrant](s"apps/${orgId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(orgId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${orgId}/groups/${groupId}/rights",Json.toJson(grant))
  }
}


