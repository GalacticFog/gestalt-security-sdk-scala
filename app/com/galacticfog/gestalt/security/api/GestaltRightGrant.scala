package com.galacticfog.gestalt.security.api

import java.util.UUID

case class GestaltRightGrant(id: UUID,
                             grantName: String,
                             grantValue: Option[String],
                             appId: UUID)
  extends GestaltResource
  with PatchSupport[GestaltRightGrant]
{
  override val href: String = s"/rights/${id}"
  override def name: String = grantName
  override def description: Option[String] = None
}

case class GestaltGrantCreate(grantName: String, grantValue: Option[String] = None)
