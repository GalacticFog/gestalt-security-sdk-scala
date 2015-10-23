package com.galacticfog.gestalt.security.api

import java.util.UUID

trait GestaltResource {
  def id: UUID
  def name: String
  def href: String
}

case class ResourceLink(id: UUID, name: String, href: String, properties: Option[Map[String,String]] = None)

