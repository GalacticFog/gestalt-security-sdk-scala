package com.galacticfog.gestalt.security.api

import play.api.Application
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class DeleteResult(wasDeleted: Boolean)

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val apiKey: String, val apiSecret: String) {

  def processResponse(response: WSResponse): Future[JsValue] = {
    response.status match {
      case x if x >= 200 && x < 300 => Future.successful(response.json)
      case x if x >= 400 && x < 500 =>
        Try(Json.parse(response.body)) match {
          case Success(json) => json.asOpt[SecurityRESTException] match {
            case Some(ex) => Future.failed(ex)
            case None => Future.failed(UnknownAPIException(x,"unknown",s"code ${response.status}: ${response.body}",""))
          }
          case Failure(ex) => Future.failed(UnknownAPIException(x,"unknown",s"code ${response.status}: ${response.body}",""))
        }
      case x => Future.failed(UnknownAPIException(x,"unknown",s"code ${response.status}: ${response.body}",""))
    }
  }

  private def removeLeadingSlash(endpoint: String) = {
    if (endpoint.startsWith("/")) endpoint.substring(1)
    else endpoint
  }

  private def genRequest(endpoint: String): WSRequestHolder = {
    val url = s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
    client.url(url)
      .withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
      )
      .withAuth(username = apiKey, password = apiSecret, scheme = WSAuthScheme.BASIC)
  }

  def get(endpoint: String): Future[JsValue] = genRequest(endpoint).get().flatMap(processResponse)

  def post(endpoint: String): Future[JsValue] = genRequest(endpoint).post("").flatMap(processResponse)

  def post(endpoint: String, payload: JsValue): Future[JsValue] = genRequest(endpoint).post(payload).flatMap(processResponse)

  def delete(endpoint: String): Future[JsValue] = genRequest(endpoint).delete().flatMap(processResponse)
}

object GestaltSecurityClient {
  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(securityConfig: GestaltSecurityConfig)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, securityConfig.protocol,securityConfig.host,securityConfig.port,securityConfig.apiKey,securityConfig.apiSecret)
}
