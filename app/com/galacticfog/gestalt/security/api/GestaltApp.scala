package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.errors.{ResourceNotFoundException, ForbiddenAPIException}
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.util.Try

case class GestaltAppCreate(name: String)

case class GestaltApp(id: UUID, name: String, orgId: UUID, isServiceApp: Boolean) extends GestaltResource {
  override val href: String = s"/apps/${id}"

  def getGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltRightGrant]] = {
    GestaltApp.getGrant(appId = id, username = username, grantName = grantName)
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltApp.listAccounts(id)
  }

  def addGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    GestaltApp.addGrant(id, username, grant)
  }

  def updateGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    GestaltApp.updateGrant(id, username, grant)
  }

  def deleteGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    GestaltApp.deleteGrant(id, username, grantName)
  }

  def listGrants(username: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltRightGrant]]] = {
    GestaltApp.listGrants(id, username)
  }

  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltApp.authorizeUser(id,creds)
  }

  def createAccount(create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
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

  def deleteApp(appId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"apps/${appId}", username, password) map { _.wasDeleted }
  }

  def authorizeUser(appId: UUID, creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postTry[GestaltAuthResponse](s"apps/${appId}/auth",creds.toJson) map {
      garTry =>
        garTry.toOption
    } recover {
      case notfound: ResourceNotFoundException => None
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

  def createAccount(appId: UUID, create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.postTry[GestaltAccount](s"apps/${appId}/accounts",Json.toJson(create))
  }

  def getById(appId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    // different semantics for this one
    client.get[GestaltApp](s"apps/${appId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  def addGrantToAccount(appId: UUID, accountId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${appId}/accounts/${accountId}/rights",Json.toJson(grant))
  }

  def addGrantToGroup(appId: UUID, groupId: UUID, grant: GestaltGrantCreate)(implicit client: GestaltSecurityClient): Future[GestaltRightGrant] = {
    client.post[GestaltRightGrant](s"apps/${appId}/groups/${groupId}/rights",Json.toJson(grant))
  }


  def deleteGrant(appId: UUID, username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    client.deleteTry(s"apps/${appId}/usernames/${username}/rights/${grantName}") map {
      _.map {
        _.wasDeleted
      }
    }
  }

  def listGrants(appId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltRightGrant]]] = {
    client.getTry[Seq[GestaltRightGrant]](s"apps/${appId}/usernames/${username}/rights")
  }

  def addGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    client.putTry[GestaltRightGrant](s"apps/${appId}/usernames/${username}/rights/${grant.grantName}",Json.toJson(grant))
  }

  def updateGrant(appId: UUID, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    addGrant(appId, username, grant)
  }
}

