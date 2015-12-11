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

  def getAppByName(appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getAppByName(id, appName)
  }

  def getDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = GestaltOrg.getDirectories(id)

  def getApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.getApps(id)
}

case class GestaltOrgSync(accounts: Seq[GestaltAccount], orgs: Seq[GestaltOrg])

case class GestaltOrgCreate(name: String, createDefaultUserGroup: Option[Boolean] = None)

case object GestaltOrg {

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
    GestaltOrg.getApps(orgId)
      .map { _.find {_.name == appName} }
  }

  def createDirectory(orgId: UUID, createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[GestaltDirectory] = {
    client.post[GestaltDirectory](s"orgs/${orgId}/directories",Json.toJson(createRequest))
  }

  def createSubOrg(parentOrgId: UUID, orgName: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.post[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(orgName)))
  }

  def createSubOrg(parentOrgId: UUID, orgName: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.postWithAuth[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(orgName)),username,password)
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

  def getApps(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get[Seq[GestaltApp]](s"orgs/${orgId}/apps") 
  }

  def getCurrentOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get[GestaltOrg]("orgs/current")
  }

  def getCurrentOrg(username: String, password: String)(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.getWithAuth[GestaltOrg]("orgs/current", username, password)
  }

  def getDirectories(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.get[Seq[GestaltDirectory]](s"orgs/${orgId}/directories") 
  }

  def getById(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    // different semantics for this one
    client.get[GestaltOrg](s"orgs/${orgId}")
      .map { b => Some(b) }
      .recover { case notFound: ResourceNotFoundException => None }
  }

  def getByFQON(fqon: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    // different semantics for this one
    client.get[GestaltOrg](s"${fqon}")
      .map { b => Some(b) }
      .recover { case notFound: ResourceNotFoundException => None }
  }

  def getById(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    // different semantics for this one
    client.getWithAuth[GestaltOrg](s"orgs/${orgId}", username = username, password = password)
      .map { b => Some(b) }
      .recover { case notFound: ResourceNotFoundException => None }
  }

  def getByFQON(fqon: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    // different semantics for this one
    client.getWithAuth[GestaltOrg](s"${fqon}", username = username, password = password)
      .map { b => Some(b) }
      .recover { case notFound: ResourceNotFoundException => None }
  }

  def addGrantToAccount(orgId: UUID, accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${orgId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(orgId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${orgId}/groups/${groupId}/rights",Json.toJson(grant))
  }
}


