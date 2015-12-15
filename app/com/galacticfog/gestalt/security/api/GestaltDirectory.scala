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
    GestaltDirectory.createAccount(id.toString, create)
  }

  @deprecated("use listAccounts", since = "2.0.0")
  def getAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    listAccounts()
  }

  def listAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltDirectory.listAccounts(id.toString)
  }

  def listGroups()(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltDirectory.listGroups(id.toString)
  }

  def listGroups(username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    GestaltDirectory.listGroups(id.toString, username, password)
  }
}

object GestaltDirectory {
  def getById(dirId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltDirectory]] = {
    // different semantics for this one
    client.get[GestaltDirectory](s"directories/${dirId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  @deprecated("use listAccounts", since = "2.0.0")
  def getAccounts(directoryId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    listAccounts(directoryId)(client)
  }

  def listAccounts(directoryId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"directories/${directoryId}/accounts")
  }

  def listGroups(directoryId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.get[Seq[GestaltGroup]](s"directories/${directoryId}/groups")
  }

  def listGroups(directoryId: String, username: String, password: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltGroup]] = {
    client.getWithAuth[Seq[GestaltGroup]](s"directories/${directoryId}/groups",username, password)
  }

  def createAccount(directoryId: String, create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccount] = {
    client.post[GestaltAccount](s"directories/${directoryId}/accounts",Json.toJson(create))
  }

}
