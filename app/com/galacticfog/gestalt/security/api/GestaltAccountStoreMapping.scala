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

case class GestaltAccountStoreMapping(id: UUID,
                                      name: String,
                                      description: String,
                                      storeType: GestaltAccountStoreType,
                                      storeId: UUID,
                                      appId: UUID,
                                      isDefaultAccountStore: Boolean,
                                      isDefaultGroupStore: Boolean)
  extends GestaltResource with PatchSupport[GestaltAccountStoreMapping]
{
  override val href: String = s"/accountStores/${id}"

  def delete()(implicit client: GestaltSecurityClient): Future[Boolean] = {
    GestaltAccountStoreMapping.delete(id)
  }
}

object GestaltAccountStoreMapping {

  def createMapping(createRequest: GestaltAccountStoreMappingCreate)(implicit client: GestaltSecurityClient): Future[GestaltAccountStoreMapping] = {
    client.post[GestaltAccountStoreMapping](s"accountStores",Json.toJson(createRequest))
  }

  def delete(mappingId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.delete(s"accountStores/${mappingId}") map {_.wasDeleted}
  }

  def getById(mappingId: UUID)(implicit client: GestaltSecurityClient): Future[Option[GestaltAccountStoreMapping]] = {
    // option semantics for this one
    client.get[GestaltAccountStoreMapping](s"accountStores/${mappingId}") map {
      b => Some(b)
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }
}

