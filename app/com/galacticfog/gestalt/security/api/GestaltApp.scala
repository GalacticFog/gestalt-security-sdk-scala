package com.galacticfog.gestalt.security.api

import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.util.{Failure, Try}

case class GestaltApp(appId: String, appName: String, org: GestaltOrg) {
  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.post(s"apps/${appId}/auth",creds.toJson) map {
      _.asOpt[GestaltAuthResponse]
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }

  def createUser(create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.post(s"apps/${appId}/users",Json.toJson(create)) map {
      json => Try{json.as[GestaltAccount]}
    } recover {
      case e: Throwable => Failure(e)
    }
  }

  def addGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = {
    client.post(s"apps/${appId}/users/${username}/rights/${grant.grantName}",Json.toJson(grant)) map {
      json => Try{json.as[GestaltRightGrant]}
    } recover {
      case e: Throwable => Failure(e)
    }
  }

  def updateGrant(username: String, grant: GestaltRightGrant)(implicit client: GestaltSecurityClient): Future[Try[GestaltRightGrant]] = addGrant(username, grant)

  def deleteGrant(username: String, grantName: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    val d = client.delete(s"apps/${appId}/users/${username}/rights/${grantName}")
    d map {
      json => Try { json.as[DeleteResult].wasDeleted }
    } recover {
      case notfound: ResourceNotFoundException => Failure(notfound)
      case e: Throwable => Failure(e)
    }
  }

  def listGrants(username: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltRightGrant]]] = {
    client.get(s"apps/${appId}/users/${username}/rights") map { js =>
      Try{js.as[Seq[GestaltRightGrant]]}
    } recover {
      case e: Throwable => Failure(e)
    }
  }

}

case object GestaltApp {
  def getAppById(appId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    client.get(s"apps/${appId}") map {
      _.asOpt[GestaltApp]
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }
}




