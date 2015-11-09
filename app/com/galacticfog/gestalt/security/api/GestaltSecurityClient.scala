package com.galacticfog.gestalt.security.api

import com.galacticfog.gestalt.security.api.errors.{APIParseException, UnknownAPIException, SecurityRESTException}
import play.api.{Logger, Application}
import play.api.libs.json.{JsString, Json, JsValue}
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class DeleteResult(wasDeleted: Boolean)

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val apiKey: String, val apiSecret: String) {

  def postTryWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    postJson(uri, username, password) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def postTryWithAuth[T](uri: String, payload: JsValue, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    postJson(uri, payload, username, password) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def postTry[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    postTryWithAuth[T](uri, username = apiKey, password = apiSecret)
  }

  def postTry[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    postTryWithAuth[T](uri, payload, username = apiKey, password = apiSecret)(fjs)
  }

  def putTryWithAuth[T](uri: String, payload: JsValue, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    putJson(uri,payload,username,password) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def putTry[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    putTryWithAuth[T](uri, payload, apiKey, apiSecret)
  }

  def getTryWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    getJson(uri,username,password) map {
      implicit json => Try{json.as[T]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def getWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[T] = {
    getTryWithAuth[T](uri,username,password) map {_.get}
  }

  def getTry[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[Try[T]] = {
    getTryWithAuth[T](uri, apiKey, apiSecret)
  }

  def deleteTry(uri: String): Future[Try[DeleteResult]] = {
    deleteTryWithAuth(uri, apiKey, apiSecret)
  }

  def deleteTryWithAuth(uri: String, username: String, password: String): Future[Try[DeleteResult]] = {
    deleteJson(uri, username, password) map {
      implicit json => Try{json.as[DeleteResult]} recoverWith errors.handleParsingErrorAsFailure
    } recover errors.convertToFailure
  }

  def delete(uri: String, username: String, password: String): Future[DeleteResult] = {
    deleteTryWithAuth(uri, username, password) map {_.get}
  }

  def get[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T]): Future[T] = {
    getTry[T](uri) map {_.get}
  }

  def processResponse(response: WSResponse): Future[JsValue] = {
    response.status match {
      case x if x >= 200 && x < 300 => Future.fromTry {
        Try {
          val json = response.json
          val str = "received json: " + json.toString
          Logger.debug(str)
          json
        } recoverWith {
          case t: Throwable => Failure(APIParseException(
            resource = "",
            message = "could not parse json from response body",
            devMessage = "Could not parse JSON from response body as expected. This is most likely a bug in the SDK; please report it",
            json = JsString(response.body)
          ))
        }
      }
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

  private def genRequest(sendingJson: Boolean, endpoint: String, username: String, password: String): WSRequestHolder = {
    val url = s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
    val rh = client.url(url).withAuth(username = username, password = password, scheme = WSAuthScheme.BASIC)
    if (sendingJson) rh.withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
      )
    else rh.withHeaders("Accept" -> "application/json")
  }

  def getJson(endpoint: String, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, username, password).get() flatMap processResponse

  def postJson(endpoint: String, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, username, password).post("") flatMap processResponse

  def postJson(endpoint: String, payload: JsValue, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, username, password).post(payload) flatMap processResponse

  def putJson(endpoint: String, payload: JsValue, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, username, password).put(payload) flatMap processResponse

  def deleteJson(endpoint: String, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, username, password).delete() flatMap processResponse
}

object GestaltSecurityClient {
  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, apiKey = apiKey, apiSecret = apiSecret)

  def apply(securityConfig: GestaltSecurityConfig)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, securityConfig.protocol,securityConfig.hostname,securityConfig.port,securityConfig.apiKey.getOrElse("anonymous"),securityConfig.apiSecret.getOrElse(""))
}
