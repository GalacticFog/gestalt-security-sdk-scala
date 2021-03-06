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
                                            storeType: GestaltAccountStoreType,
                                            accountStoreId: UUID,
                                            isDefaultAccountStore: Boolean,
                                            isDefaultGroupStore: Boolean,
                                            description: Option[String] = None) {
  def this(name: String, description: Option[String], dir: GestaltDirectory, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, storeType = DIRECTORY, accountStoreId = dir.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore, description = description)
  def this(name: String, description: Option[String], group: GestaltGroup, app: GestaltApp, isDefaultAccountStore: Boolean, isDefaultGroupStore: Boolean) =
    this(name, storeType = GROUP, accountStoreId = group.id, isDefaultAccountStore = isDefaultAccountStore, isDefaultGroupStore = isDefaultGroupStore, description = description)
}

case class GestaltAccountStoreMapping(id: UUID,
                                      name: String,
                                      description: Option[String],
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

  def delete(mappingId: UUID)(implicit client: GestaltSecurityClient): Future[Boolean] = {
    client.deleteDR(s"accountStores/${mappingId}") map {_.wasDeleted}
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
