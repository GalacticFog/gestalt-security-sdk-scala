package com.galacticfog.gestalt.security.api

import com.galacticfog.gestalt.security.api.errors.{UnknownAPIException, SecurityRESTException}
import play.api.Application
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class DeleteResult(wasDeleted: Boolean)

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val apiKey: String, val apiSecret: String) {

  def postWithAuth(uri: String, username: String, password: String): Future[JsValue] = {
    val url = s"${protocol}://${hostname}:${port}/${removeLeadingSlash(uri)}"
    client.url(url)
      .withAuth(username = username, password = password, scheme = WSAuthScheme.BASIC)
      .withHeaders( "Accept" -> "application/json" )
      .post("")
      .flatMap(processResponse)
  }

  def postTry[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    postJson(uri,payload) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def putTry[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    putJson(uri,payload) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def getTry[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    getJson(uri) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def deleteTry(uri: String): Future[Try[DeleteResult]] = {
    deleteJson(uri) map {
      implicit json => Try{json.as[DeleteResult]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def get[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[T] = {
    getTry[T](uri) map {_.get}
  }

  def post[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[T] = {
    postTry[T](uri,payload) map {_.get}
  }

  def put[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[T] = {
    putTry[T](uri,payload) map {_.get}
  }

  def delete(uri: String)(implicit fjs : play.api.libs.json.Reads[DeleteResult]): Future[DeleteResult] = {
    deleteTry(uri) map {_.get}
  }

  def processResponse(response: WSResponse): Future[JsValue] = {
    response.status match {
      case x if x >= 200 && x < 300 => Future.successful(response.json)
      case x if x >= 400 && x < 500 =>
        Try(Json.parse(response.body)) match {
          case Success(json) => json.asOpt[SecurityRESTException] match {
            case Some(ex) => Future.failed(ex)
            case None => Future.failed(UnknownAPIException(x,"unknown",s"could not parse to SecurityRESTException: ${response.body}",""))
          }
          case Failure(ex) =>
            Future.failed(UnknownAPIException(x,"unknown",s"could not parse to JSON: ${response.body}",""))
        }
      case x => Future.failed(UnknownAPIException(x,"unknown",s"unhandled error, code: ${response.status}, body: ${response.body}",""))
    }
  }

  private def removeLeadingSlash(endpoint: String) = {
    if (endpoint.startsWith("/")) endpoint.substring(1)
    else endpoint
  }

  private def genRequest(sendingJson: Boolean, endpoint: String): WSRequestHolder = {
    val url = s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
    val rh = client.url(url).withAuth(username = apiKey, password = apiSecret, scheme = WSAuthScheme.BASIC)
    if (sendingJson) rh.withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
      )
    else rh.withHeaders("Accept" -> "application/json")
  }

  def getJson(endpoint: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint).get() flatMap processResponse

  def postJson(endpoint: String, payload: JsValue): Future[JsValue] =
    genRequest(sendingJson = true, endpoint).post(payload) flatMap processResponse

  def putJson(endpoint: String, payload: JsValue): Future[JsValue] =
    genRequest(sendingJson = true, endpoint).put(payload) flatMap processResponse

  def deleteJson(endpoint: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint).delete() flatMap processResponse
}

object GestaltSecurityClient {
  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(securityConfig: GestaltSecurityConfig)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, securityConfig.protocol,securityConfig.hostname,securityConfig.port,securityConfig.apiKey.getOrElse("anonymous"),securityConfig.apiSecret.getOrElse(""))
}
