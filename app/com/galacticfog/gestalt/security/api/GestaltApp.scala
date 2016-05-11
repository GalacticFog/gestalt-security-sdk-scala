package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.errors.{UnauthorizedAPIException, ResourceNotFoundException, ForbiddenAPIException}
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class GestaltAppCreate(name: String)

case class GestaltApp(id: UUID, name: String, orgId: UUID, isServiceApp: Boolean) extends GestaltResource {
  override val href: String = s"/apps/${id}"

  def getGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltRightGrant]] = {
    GestaltApp.getGrant(appId = id, username = username, grantName = grantName)
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] =
    GestaltApp.listAccounts(id)

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] =
    GestaltApp.listGroups(id)


  def getAccountByUsername(username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    GestaltApp.getAccountByUsername(id, username)
  }

  def getAccountById(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    GestaltApp.getAccountById(id, accountId)
  }

  def getGroupByName(groupName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltApp.getGroupByName(id, groupName)
  }

  def getGroupById(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    GestaltApp.getGroupById(id, groupId)
  }

  def addGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    GestaltApp.addGrant(id, username, grant)
  }

  def updateGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    GestaltApp.updateGrant(id, username, grant)
  }

  def deleteGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    GestaltApp.deleteGrant(id, username, grantName)
  }

  def listGroupGrants(groupName: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listGroupGrantsByUsername(id, groupName)
  }

  def listGroupGrants(groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listGroupGrants(id, groupId)
  }

  def listAccountGrants(username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listAccountGrantsByUsername(id, username)
  }

  def listAccountGrants(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listAccountGrants(id, accountId)
  }

  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltApp.authorizeUser(id,creds)
  }

  def createAccount(create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    GestaltApp.createAccount(id, create)
  }

  def createGroup(create: GestaltGroupCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    GestaltApp.createGroup(id, create)
  }

  def mapAccountStore(create: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    GestaltApp.mapAccountStore(id, create)
  }

  def listAccountStores()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    GestaltApp.listAccountStores(id)
  }

  ////////////////////////////////////////
  // account UUID approaches
  ////////////////////////////////////////
  def addAccountGrant(accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    GestaltApp.addGrantToAccount(appId = id, accountId = accountId, grant)
  }

  ////////////////////////////////////////
  // group UUID approaches
  ////////////////////////////////////////
  def addGroupGrant(groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    GestaltApp.addGrantToGroup(appId = id, groupId = groupId, grant)
  }

}

case object GestaltApp {

  def getAccountById(appId: UUID, accountId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"apps/${appId}/accounts/${accountId}")
  }

  def getGroupById(appId: UUID, groupId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"apps/${appId}/groups/${groupId}")
  }

  def listGroups(appId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"apps/${appId}/groups")
  }


  def getGroupByName(appId: UUID, groupName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"apps/${appId}/groupnames/${groupName}")
  }

  def getAccountByUsername(appId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"apps/${appId}/usernames/${username}")
  }

  def deleteApp(appId: UUID, creds: GestaltAPICredentials)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"apps/${appId}", creds) map { _.wasDeleted }
  }

  def authorizeUser(appId: UUID, creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.post[GestaltAuthResponse](s"apps/${appId}/auth",creds.toJson) map {Some(_)} recoverWith {
      case notfound: ResourceNotFoundException => Future.successful(None)
      case authc: UnauthorizedAPIException =>
        Logger.warn("GestaltApp.authorizeUser(): caught UnauthorziedAPIException; this is likely because the GestaltSecurityClient is misconfigured with invalid credentials.")
        Future.failed(authc)
      case authz: ForbiddenAPIException =>
        Logger.warn("GestaltApp.authorizeUser(): caught ForbiddenAPIException; the credentials provided ot the GestaltSecurityClient do not have appropriate permissions for authorizing users against this application.")
        Future.failed(authz)
    }
  }

  def listAccountStores(appId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    client.get[Seq[GestaltAccountStoreMapping]](s"apps/${appId}/accountStores")
  }

  def getGrant(appId: UUID, username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltRightGrant]] = {
    client.get[GestaltRightGrant](s"apps/${appId}/accounts/${username}/rights/${grantName}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException if notFound.resource.endsWith(s"/rights/${grantName}") => None
    }
  }

  def listAccounts(appId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"apps/${appId}/accounts")
  }

  def createAccount(appId: UUID, create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"apps/${appId}/accounts",Json.toJson(create))
  }

  def createGroup(appId: UUID, create: GestaltGroupCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    client.post[GestaltGroup](s"apps/${appId}/groups",Json.toJson(create))
  }

  def mapAccountStore(appId: UUID, createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    client.post[GestaltAccountStoreMapping](s"apps/${appId}/accountStores",Json.toJson(createRequest))
  }

  def getById(appId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    client.getOpt[GestaltApp](s"apps/${appId}")
  }

  def addGrantToAccount(appId: UUID, accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${appId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(appId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${appId}/groups/${groupId}/rights",Json.toJson(grant))
  }

  def deleteGrant(appId: UUID, username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"apps/${appId}/usernames/${username}/rights/${grantName}") map {_.wasDeleted}
  }

  def listAccountGrantsByUsername(appId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/usernames/${username}/rights")
  }

  def listAccountGrants(appId: UUID, accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/accounts/${accountId}/rights")
  }

  def listGroupGrantsByUsername(appId: UUID, groupName: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/groupnames/${groupName}/rights")
  }

  def listGroupGrants(appId: UUID, groupId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/groups/${groupId}/rights")
  }

  def addGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.put[GestaltRightGrant](s"apps/${appId}/usernames/${username}/rights/${grant.grantName}",Json.toJson(grant))
  }

  def updateGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    addGrant(appId, username, grant)
  }
}

