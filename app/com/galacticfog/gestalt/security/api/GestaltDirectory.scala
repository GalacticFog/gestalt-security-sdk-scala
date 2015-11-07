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

  def createAccount(create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    GestaltDirectory.createAccount(id.toString, create)
  }

  def getAccounts()(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    GestaltDirectory.getAccounts(id.toString)
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

  def getAccounts(directoryId: String)(implicit client: GestaltSecurityClient): Future[Seq[GestaltAccount]] = {
    client.get[Seq[GestaltAccount]](s"directories/${directoryId}/accounts")
  }

  def createAccount(directoryId: String, create: GestaltAccountCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccount]] = {
    client.postTry[GestaltAccount](s"directories/${directoryId}/accounts",Json.toJson(create))
  }


}
