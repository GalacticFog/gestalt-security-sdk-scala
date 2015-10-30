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

  def createGroup(orgId: UUID, createRequest: GestaltGroupCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltGroup]] = {
    client.postTry[GestaltGroup](s"orgs/${orgId}/groups", Json.toJson(createRequest))
  }

  def createGroup(orgId: UUID, createRequest: GestaltGroupCreateWithRights, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[GestaltGroup]] = {
    client.postTryWithAuth[GestaltGroup](s"orgs/${orgId}/groups", Json.toJson(createRequest), username, password)
  }

  def createAccount(orgId: UUID, createRequest: GestaltAccountCreateWithRights)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.postTry[GestaltAccount](s"orgs/${orgId}/accounts", Json.toJson(createRequest))
  }

  def createAccount(orgId: UUID, createRequest: GestaltAccountCreateWithRights, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.postTryWithAuth[GestaltAccount](s"orgs/${orgId}/accounts", Json.toJson(createRequest), username, password)
  }

  def authorizeFrameworkUser(apiKey: String, apiSecret: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postTryWithAuth[GestaltAuthResponse](s"auth", username = apiKey, password = apiSecret)
      .map { _.toOption }
  }

  def authorizeFrameworkUser(orgFQON: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postTryWithAuth[GestaltAuthResponse](s"${orgFQON}/auth", username = username, password = password)
      .map { _.toOption }
  }

  def authorizeFrameworkUser(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.postTryWithAuth[GestaltAuthResponse](s"orgs/${orgId}/auth", username = username, password = password)
      .map { _.toOption }
  }

  def getAppByName(orgId: String, appName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getApps(orgId)
      .map { _.find {_.name == appName} }
  }

  def createDirectory(orgId: String, createRequest: GestaltDirectoryCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltDirectory]] = {
    client.postTry[GestaltDirectory](s"orgs/${orgId}/directories",Json.toJson(createRequest))
  }

  def createSubOrg(parentOrgId: UUID, orgName: String)(implicit client: GestaltSecurityClient): Future[Try[GestaltOrg]] = {
    client.postTry[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(orgName)))
  }

  def createSubOrg(parentOrgId: UUID, orgName: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[GestaltOrg]] = {
    client.postTryWithAuth[GestaltOrg](s"orgs/${parentOrgId}",Json.toJson(GestaltOrgCreate(orgName)),username,password)
  }

  def deleteOrg(orgId: UUID)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    client.deleteTry(s"orgs/${orgId}")
      .map { _.map { _.wasDeleted } }
  }

  def deleteOrg(orgId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    client.deleteTryWithAuth(s"orgs/${orgId}", username = username, password = password)
      .map { _.map { _.wasDeleted } }
  }

  def createApp(orgId: String, createRequest: GestaltAppCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltApp]] = {
    client.postTry[GestaltApp](s"orgs/${orgId}/apps",Json.toJson(createRequest))
  }

  def getOrgs(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Try[Seq[GestaltOrg]]] = {
    client.getTryWithAuth[Seq[GestaltOrg]]("orgs",username,password)
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
    client.get[GestaltOrg](s"orgs/${orgId}")
      .map { b => Some(b) }
      .recover { case notFound: ResourceNotFoundException => None }
  }
}


