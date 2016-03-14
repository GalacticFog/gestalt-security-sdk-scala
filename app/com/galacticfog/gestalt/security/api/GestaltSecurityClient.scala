package com.galacticfog.gestalt.security.api

import com.galacticfog.gestalt.security.api.errors._
import play.api.http.MimeTypes
import play.api.{Logger, Application}
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class DeleteResult(wasDeleted: Boolean)

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val apiKey: String, val apiSecret: String) {

  def validate[T](json: JsValue)(implicit m: reflect.Manifest[T], rds: Reads[T]): T = {
    json.validate[T] match {
      case s: JsSuccess[T] => s.get
      case e: JsError => throw new APIParseException(
        resource = "",
        message = "invalid payload",
        devMessage = s"Error parsing a successful API response; was expecting JSON representation of SDK object ${m.toString}. Likely culprit is a version mismatch between the client and the API. Please contact the developers.",
        json = json
      )
    }
  }

  def patchWithAuth[T](uri: String, payload: JsValue, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    patchJson(uri, payload, username, password) map validate[T]
  }

  def postWithAuth[T](uri: String, payload: JsValue, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, payload, username, password) map validate[T]
  }

  def postWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, username, password) map validate[T]
  }

  def post[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postWithAuth[T](uri = uri, username = apiKey, password = apiSecret)
  }

  def post[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, payload, username = apiKey, password = apiSecret) map validate[T]
  }

  def postForm[T](endpoint: String, fields: Map[String, String])(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    client
      .url(genUri(endpoint))
      .post(fields.mapValues(Seq(_))) flatMap processResponse map validate[T]
  }

  def putWithAuth[T](uri: String, payload: JsValue, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    putJson(uri,payload,username,password) map validate[T]
  }

  def put[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    putWithAuth[T](uri, payload, apiKey, apiSecret)
  }

  def getWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri,username,password) map validate[T]
  }

  def delete(uri: String): Future[DeleteResult] = {
    delete(uri, apiKey, apiSecret)
  }

  def delete(uri: String, username: String, password: String): Future[DeleteResult] = {
    deleteJson(uri, username, password) map validate[DeleteResult]
  }

  def deleteJson[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    deleteJson(uri, apiKey, apiSecret) map validate[T]
  }

  def get[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri, username = apiKey, password = apiSecret) map validate[T]
  }

  def getOpt[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[Option[T]] = {
    getJson(uri, username = apiKey, password = apiSecret) map {
      j => Some(validate[T](j))
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  def getOptWithAuth[T](uri: String, username: String, password: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[Option[T]] = {
    getJson(uri, username = username, password = password) map {
      j => Some(validate[T](j))
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  def processResponse(response: WSResponse): Future[JsValue] = {
    response.status match {
      case x if x >= 200 && x < 300 => Future.fromTry {
        Try {
          val json = response.json
          val str = "received json: " + json.toString
          Logger.trace(str)
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
    val rh = client.url(genUri(endpoint)).withAuth(username = username, password = password, scheme = WSAuthScheme.BASIC)
    if (sendingJson) rh.withHeaders(
        "Content-Type" -> MimeTypes.JSON,
        "Accept" -> MimeTypes.JSON
      )
    else rh.withHeaders("Accept" -> MimeTypes.JSON)
  }

  private def genUri(endpoint: String): String = {
    s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
  }

  def getJson(endpoint: String, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, username, password).get() flatMap processResponse

  def postJson(endpoint: String, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, username, password).post("") flatMap processResponse

  def postJson(endpoint: String, payload: JsValue, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, username, password).post(payload) flatMap processResponse

  def patchJson(endpoint: String, payload: JsValue, username: String, password: String): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, username, password).patch(payload) flatMap processResponse

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
