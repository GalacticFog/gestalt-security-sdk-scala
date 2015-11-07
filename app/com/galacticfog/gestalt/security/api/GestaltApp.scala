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
    GestaltApp.getGrant(appId = id.toString, username = username, grantName = grantName)
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltApp.listAccounts(id.toString)
  }


  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    GestaltApp.authorizeUser(id,creds)
  }

  def createAccount(create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    GestaltApp.createAccount(id.toString, create)
  }

  def addGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    GestaltApp.addGrant(id.toString, username, grant)
  }

  def updateGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    GestaltApp.updateGrant(id.toString, username, grant)
  }

  def deleteGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    GestaltApp.deleteGrant(id.toString, username, grantName)
  }

  def listGrants(username: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltRightGrant]]] = {
    GestaltApp.listGrants(id.toString, username)
  }

  def listAccountStores()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    GestaltApp.listAccountStores(id.toString)
  }

}

case object GestaltApp {

  def authorizeUser(appId: UUID, creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postTry[GestaltAuthResponse](s"apps/${appId}/auth",creds.toJson) map {
      garTry =>
        garTry.toOption
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }

  def listAccountStores(appId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccountStoreMapping]] = {
    client.get[Seq[GestaltAccountStoreMapping]](s"apps/${appId}/accountStores")
  }

  def getGrant(appId: String, username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltRightGrant]] = {
    client.get[GestaltRightGrant](s"apps/${appId}/accounts/${username}/rights/${grantName}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException if notFound.resource.endsWith(s"/rights/${grantName}") => None
    }
  }

  def listAccounts(appId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"apps/${appId}/accounts")
  }

  def deleteGrant(appId: String, username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    client.deleteTry(s"apps/${appId}/accounts/${username}/rights/${grantName}") map {
      _.map {
        _.wasDeleted
      }
    }
  }

  def createAccount(appId: String, create: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.postTry[GestaltAccount](s"apps/${appId}/accounts",Json.toJson(create))
  }

  def listGrants(appId: String, username: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltRightGrant]]] = {
    client.getTry[Seq[GestaltRightGrant]](s"apps/${appId}/accounts/${username}/rights")
  }

  def getById(appId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    // different semantics for this one
    client.get[GestaltApp](s"apps/${appId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  def addGrant(appId: String, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    client.putTry[GestaltRightGrant](s"apps/${appId}/accounts/${username}/rights/${grant.grantName}",Json.toJson(grant))
  }

  def updateGrant(appId: String, username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    addGrant(appId, username, grant)
  }
}

