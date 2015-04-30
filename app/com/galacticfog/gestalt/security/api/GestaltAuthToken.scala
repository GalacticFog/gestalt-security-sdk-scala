package com.galacticfog.gestalt.security.api

import play.api.libs.json.{Json, JsValue}
import com.galacticfog.gestalt.security.api.json.JsonImports.basicAuthTokenFormat

trait GestaltAuthToken {
  def toJson: JsValue
}

case class GestaltBasicCredsToken(username: String, password: String) extends GestaltAuthToken {
  override def toJson: JsValue = Json.toJson(this)
}
