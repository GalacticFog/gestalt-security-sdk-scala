package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.errors.ResourceNotFoundException
import com.galacticfog.gestalt.security.api.json.JsonImports._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

sealed trait GestaltAccountStoreType {
  def label: String
}
case object DIRECTORY extends GestaltAccountStoreType {
  def label: String = "DIRECTORY"
}
case object GROUP extends GestaltAccountStoreType {
  def label: String = "GROUP"
}

case class GestaltAccountStoreMappingCreate(name: String,
                                            description: String,
                                            storeType: GestaltAccountStoreType,
                                            accountStoreId: UUID,
                                            appId: UUID,
                                            isDefaultAccountStore: Boolean,
                                            isDefaultGroupStore: Boolean) {
  def this(name: String, description: String, dir: GestaltDirectory, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, description, storeType = DIRECTORY, accountStoreId = dir.id, appId = app.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore)
  def this(name: String, description: String, group: GestaltGroup, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, description, storeType = GROUP, accountStoreId = group.id, appId = app.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore)
}

case class GestaltOrgAccountStoreMappingCreate(name: String,
                                               description: String,
                                               storeType: GestaltAccountStoreType,
                                               accountStoreId: UUID,
                                               isDefaultAccountStore: Boolean,
                                               isDefaultGroupStore: Boolean) {
  def this(name: String, description: String, dir: GestaltDirectory, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, description, storeType = DIRECTORY, accountStoreId = dir.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore)
  def this(name: String, description: String, group: GestaltGroup, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, description, storeType = GROUP, accountStoreId = group.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore)
}

case class GestaltAccountStoreMappingUpdate(id: UUID,
                                            name: String,
                                            description: String,
                                            isDefaultAccountStore: Boolean,
                                            isDefaultGroupStore: Boolean)

case class GestaltAccountStoreMapping(id: UUID,
                                      name: String,
                                      description: String,
                                      storeType: GestaltAccountStoreType,
                                      storeId: UUID,
                                      appId: UUID,
                                      isDefaultAccountStore: Boolean,
                                      isDefaultGroupStore: Boolean) extends GestaltResource {

  override val href: String = s"/accountStoreMappings/${id}"

  def update(updateRequest: GestaltAccountStoreMappingUpdate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccountStoreMapping]] = {
    GestaltAccountStoreMapping.update(id.toString, updateRequest)
  }

  def delete()(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    GestaltAccountStoreMapping.delete(id.toString)
  }
}

object GestaltAccountStoreMapping {

  def update(mappingId: String, updateRequest: GestaltAccountStoreMappingUpdate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccountStoreMapping]] = {
    client.putTry[GestaltAccountStoreMapping](s"accountStoreMappings/${mappingId}",Json.toJson(updateRequest))
  }

  def createMapping(createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[Try[GestaltAccountStoreMapping]] = {
    client.postTry[GestaltAccountStoreMapping](s"accountStoreMappings",Json.toJson(createRequest))
  }

  def delete(mappingId: String)(implicit client: GestaltSecurityClient): Future[Try[Boolean]] = {
    client.deleteTry(s"accountStoreMappings/${mappingId}") map {
      _.map {
        _.wasDeleted
      }
    }
  }

  def getById(mappingId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccountStoreMapping]] = {
    // option semantics for this one
    client.get[GestaltAccountStoreMapping](s"accountStoreMappings/${mappingId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }
}

