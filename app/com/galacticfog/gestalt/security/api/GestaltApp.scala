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

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltApp.listAccounts(id)
  }

  def getAccountByUsername(username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    GestaltApp.getAccountByUsername(id, username)
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

  def listGrants(username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listGrants(id, username)
  }

  def listGrants(accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    GestaltApp.listGrants(id, accountId)
  }

  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltApp.authorizeUser(id,creds)
  }

  def createAccount(create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    GestaltApp.createAccount(id, create)
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

  def getAccountByUsername(appId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"apps/${appId}/usernames/${username}")
  }

  def deleteApp(appId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"apps/${appId}", username, password) map { _.wasDeleted }
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

  def listGrants(appId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/usernames/${username}/rights")
  }

  def listGrants(appId: UUID, accountId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltRightGrant]] = {
    client.get[Seq[GestaltRightGrant]](s"apps/${appId}/accounts/${accountId}/rights")
  }

  def addGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.put[GestaltRightGrant](s"apps/${appId}/usernames/${username}/rights/${grant.grantName}",Json.toJson(grant))
  }

  def updateGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    addGrant(appId, username, grant)
  }
}

