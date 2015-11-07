package com.galacticfog.gestalt.security.api

import java.util.UUID

trait GestaltResource {
  def id: UUID
  def name: String
  def href: String

  def getLink(): ResourceLink = ResourceLink(id = id, name = name, href = href, properties = None)
}

case class ResourceLink(id: UUID, name: String, href: String, properties: Option[Map[String,String]] = None)

