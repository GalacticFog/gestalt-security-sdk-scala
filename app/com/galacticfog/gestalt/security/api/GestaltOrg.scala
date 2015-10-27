package com.galacticfog.gestalt.security.api

import java.util.UUID

import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.Future
import com.galacticfog.gestalt.security.api.json.JsonImports._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Try}
import errors._

case class GestaltOrg(id: UUID, name: String, fqon: String, parent: Option[ResourceLink], children: Seq[ResourceLink]) extends GestaltResource {
  override val href: String = s"/orgs/${id}"

  def createDirectory(createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltDirectory]] = {
    GestaltOrg.createDirectory(id.toString, createRequest)
  }

  def createApp(createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltApp]] =
    GestaltOrg.createApp(id.toString, createRequest)

  def getAppByName(appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getAppByName(id.toString, appName)
  }

  def getDirectories()(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = GestaltOrg.getDirectories(id.toString)

  def getApps()(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = GestaltOrg.getApps(id.toString)
}

case class GestaltOrgCreate(orgName: String)

case object GestaltOrg {

  def authorizeFrameworkUser(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth(s"auth", username = username, password = password) map { js =>
      Logger.info(s"security returned 200: ${js}")
      js.asOpt[GestaltAuthResponse]
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }

  def authorizeFrameworkUser(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth(s"${orgFQON}/auth", username = username, password = password) map { js =>
      Logger.info(s"security returned 200: ${js}")
      js.asOpt[GestaltAuthResponse]
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }

  def authorizeFrameworkUser(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postWithAuth(s"orgs/${orgId}/auth", username = username, password = password) map { js =>
      Logger.info(s"security returned 200: ${js}")
      js.asOpt[GestaltAuthResponse]
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }

  def getAppByName(orgId: String, appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getApps(orgId) map {
      _.find {_.name == appName}
    }
  }

  def createDirectory(orgId: String, createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltDirectory]] = {
    client.postTry[GestaltDirectory](s"orgs/${orgId}/directories",Json.toJson(createRequest))
  }

  def createSubOrg(parentOrgId: UUID, orgName: String)(implicit client: GestaltSecurityClient): Future[Try[GestaltOrg]] = {
    client.postTry[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(orgName)))
  }

  def createApp(orgId: String, createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltApp]] = {
    client.postTry[GestaltApp](s"orgs/${orgId}/apps",Json.toJson(createRequest))
  }

  def getApps(orgId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get[Seq[GestaltApp]](s"orgs/${orgId}/apps") 
  }

  def getCurrentOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get[GestaltOrg]("orgs/current")
  }

  def getDirectories(orgId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltDirectory]] = {
    client.get[Seq[GestaltDirectory]](s"orgs/${orgId}/directories") 
  }

  def getById(orgId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltOrg]] = {
    // different semantics for this one
    client.get[GestaltOrg](s"orgs/${orgId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }
}


