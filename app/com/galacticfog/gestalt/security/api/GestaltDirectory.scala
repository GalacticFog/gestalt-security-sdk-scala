package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.errors.ResourceNotFoundException
import play.api.libs.json.{JsValue, JsObject, Json}

import scala.concurrent.Future
import scala.util.{Failure, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._
import scala.concurrent.ExecutionContext.Implicits.global
import errors._

case class GestaltDirectoryCreate(name: String, description: Option[String], config: Option[JsValue])

case class GestaltDirectory(id: UUID, name: String, description: String, orgId: UUID) extends GestaltResource {
  override val href: String = s"/directories/${id}"

  def createAccount(create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    GestaltDirectory.createAccount(id, create)
  }

  def createGroup(create: GestaltGroupCreate)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    GestaltDirectory.createGroup(id, create)
  }

  def getAccountByUsername(username: String)(implicit client: GestaltSecurityClient) = {
    GestaltDirectory.getAccountByUsername(id, username)
  }

  def getGroupByName(groupName: String)(implicit client: GestaltSecurityClient) = {
    GestaltDirectory.getGroupByName(id, groupName)
  }

  @deprecated("use listAccounts", since = "2.0.0")
  def getAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    listAccounts()
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltDirectory.listAccounts(id)
  }

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltDirectory.listGroups(id)
  }

  def listGroups(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltDirectory.listGroups(id, username, password)
  }

}

object GestaltDirectory {

  def deleteDirectory(dirId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"directories/${dirId}") map { _.wasDeleted }
  }

  def createGroup(dirId: UUID, create: GestaltGroupCreate)(implicit client: GestaltSecurityClient): Future[GestaltGroup] = {
    client.post[GestaltGroup](s"directories/${dirId}/groups", Json.toJson(create))
  }

  def getAccountByUsername(dirId: UUID, username: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccount]] = {
    client.getOpt[GestaltAccount](s"directories/${dirId}/usernames/${username}")
  }

  def getGroupByName(dirId: UUID, groupName: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltGroup]] = {
    client.getOpt[GestaltGroup](s"directories/${dirId}/groupnames/${groupName}")
  }

  def getById(dirId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltDirectory]] = {
    // different semantics for this one
    client.get[GestaltDirectory](s"directories/${dirId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  @deprecated("use listAccounts", since = "2.0.0")
  def getAccounts(directoryId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    listAccounts(directoryId)(client)
  }

  def listAccounts(directoryId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"directories/${directoryId}/accounts")
  }

  def listGroups(directoryId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"directories/${directoryId}/groups")
  }

  def listGroups(directoryId: UUID, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getWithAuth[Seq[GestaltGroup]](s"directories/${directoryId}/groups",username, password)
  }

  def createAccount(directoryId: UUID, create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"directories/${directoryId}/accounts",Json.toJson(create))
  }

}
