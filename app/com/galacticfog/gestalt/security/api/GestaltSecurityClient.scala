package com.galacticfog.gestalt.security.api

import play.api.Application
import play.api.libs.json.JsValue
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class UnauthorizedAPIException(resp: String) extends Throwable(resp)
case class ForbiddenAPIException(resp: String) extends Throwable(resp)
case class UnknownAPIException(resp: String) extends Throwable(resp)
case class ResourceNotFoundException(url: String) extends Throwable("resource not found: " + url)

sealed abstract class Protocol
case object HTTP  extends Protocol {override def toString() = "http"}
case object HTTPS extends Protocol {override def toString() = "https"}

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val apiKey: String, val apiSecret: String) {

  def processResponse(response: WSResponse): JsValue = {
    response.status match {
      case x if x >= 200 && x < 300 => response.json
      case x if x == 401 => throw new UnauthorizedAPIException(response.body)
      case x if x == 403 => throw new ForbiddenAPIException(response.body)
      case x if x == 404 => throw new ResourceNotFoundException(response.body)
      case _ => throw new UnknownAPIException(s"${response.status}: ${response.body}")
    }
  }

  private def removeLeadingSlash(endpoint: String) = {
    if (endpoint.startsWith("/")) endpoint.substring(1)
    else endpoint
  }

  private def genRequest(endpoint: String): WSRequestHolder = {
    client.url(s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}")
      .withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
      )
      .withAuth(username = apiKey, password = apiSecret, scheme = WSAuthScheme.BASIC)
  }

  def get(endpoint: String): Future[JsValue] = genRequest(endpoint).get().map(processResponse)

  def post(endpoint: String): Future[JsValue] = genRequest(endpoint).post("").map(processResponse)

  def post(endpoint: String, payload: JsValue): Future[JsValue] = genRequest(endpoint).post(payload).map(processResponse)

  def delete(endpoint: String): Future[JsValue] = genRequest(endpoint).delete().map(processResponse)
}

object GestaltSecurityClient {
  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)
}
