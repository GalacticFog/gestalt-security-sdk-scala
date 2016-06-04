package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.errors.ResourceNotFoundException
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.{Failure, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.concurrent.ExecutionContext.Implicits.global
import errors._

sealed trait DirectoryType {
  def label: String
}

final case object DIRECTORY_TYPE_INTERNAL extends DirectoryType { val label = "INTERNAL" }
final case object DIRECTORY_TYPE_LDAP extends DirectoryType { val label = "LDAP" }

case class GestaltDirectoryCreate(name: String, directoryType: DirectoryType, description: Option[String] = None, config: Option[JsValue] = None)

case class GestaltDirectory(id: UUID, name: String, description: Option[String], orgId: UUID) extends GestaltResource {

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

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltDirectory.listAccounts(id)
  }

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltDirectory.listGroups(id)
  }

}

object GestaltDirectory {

  def deleteDirectory(dirId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"directories/${dirId}") map { _.wasDeleted }
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

  def listAccounts(directoryId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"directories/${directoryId}/accounts")
  }

  def listGroups(directoryId: UUID)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"directories/${directoryId}/groups")
  }

  def createAccount(directoryId: UUID, create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"directories/${directoryId}/accounts",Json.toJson(create))
  }
}
