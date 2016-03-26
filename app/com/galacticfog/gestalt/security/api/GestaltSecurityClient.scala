package com.galacticfog.gestalt.security.api

import com.galacticfog.gestalt.security.api.errors._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.{Logger, Application}
import play.api.libs.json._
import play.api.libs.ws._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class DeleteResult(wasDeleted: Boolean)

class GestaltSecurityClient(val client: WSClient, val protocol: Protocol, val hostname: String, val port: Int, val creds: GestaltAPICredentials) {

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

  def patch[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    patchJson(uri, payload, creds) map validate[T]
  }

  def patchWithAuth[T](uri: String, payload: JsValue, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    patchJson(uri, payload, creds) map validate[T]
  }

  def postWithAuth[T](uri: String, payload: JsValue, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, payload, creds) map validate[T]
  }

  def postWithAuth[T](uri: String, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, creds) map validate[T]
  }

  def post[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postWithAuth[T](uri = uri, creds)
  }

  def post[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, payload, creds) map validate[T]
  }

  def postForm[T](endpoint: String, fields: Map[String, String])(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    client
      .url(genUri(endpoint))
      .post(fields.mapValues(Seq(_))) flatMap processResponse map validate[T]
  }

  def putWithAuth[T](uri: String, payload: JsValue, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    putJson(uri,payload,creds) map validate[T]
  }

  def put[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    putWithAuth[T](uri, payload, creds)
  }

  def getWithAuth[T](uri: String, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri,creds) map validate[T]
  }

  def delete(uri: String): Future[DeleteResult] = {
    delete(uri, creds)
  }

  def delete(uri: String, creds: GestaltAPICredentials): Future[DeleteResult] = {
    deleteJson(uri, creds) map validate[DeleteResult]
  }

  def deleteJson[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    deleteJson(uri, creds) map validate[T]
  }

  def get[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri, creds) map validate[T]
  }

  def getOpt[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[Option[T]] = {
    getJson(uri, creds) map {
      j => Some(validate[T](j))
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }

  def getOptWithAuth[T](uri: String, creds: GestaltAPICredentials)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[Option[T]] = {
    getJson(uri, creds) map {
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

  private def genRequest(sendingJson: Boolean, endpoint: String, creds: GestaltAPICredentials): WSRequestHolder = {
    val rh = client.url(genUri(endpoint)).withHeaders( HeaderNames.AUTHORIZATION -> creds.headerValue )
    if (sendingJson) rh.withHeaders(
        "Content-Type" -> MimeTypes.JSON,
        "Accept" -> MimeTypes.JSON
      )
    else rh.withHeaders("Accept" -> MimeTypes.JSON)
  }

  private def genUri(endpoint: String): String = {
    s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
  }

  def getJson(endpoint: String, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, creds).get() flatMap processResponse

  def postJson(endpoint: String, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, creds).post("") flatMap processResponse

  def postJson(endpoint: String, payload: JsValue, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, creds).post(payload) flatMap processResponse

  def patchJson(endpoint: String, payload: JsValue, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, creds).patch(payload) flatMap processResponse

  def putJson(endpoint: String, payload: JsValue, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = true, endpoint, creds).put(payload) flatMap processResponse

  def deleteJson(endpoint: String, creds: GestaltAPICredentials): Future[JsValue] =
    genRequest(sendingJson = false, endpoint, creds).delete() flatMap processResponse
}

object GestaltSecurityClient {
  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, creds = GestaltBasicCredentials(apiKey, apiSecret))

  def apply(wsclient: WSClient, protocol: Protocol, hostname: String, port: Int, creds: GestaltAPICredentials) =
    new GestaltSecurityClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, creds = creds)

  def apply(protocol: Protocol, hostname: String, port: Int, apiKey: String, apiSecret: String)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, creds = GestaltBasicCredentials(apiKey, apiSecret))

  def apply(protocol: Protocol, hostname: String, port: Int, creds: GestaltAPICredentials)(implicit app: Application) =
    new GestaltSecurityClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, creds = creds)

  def apply(securityConfig: GestaltSecurityConfig)(implicit app: Application) =
    new GestaltSecurityClient(
      client = WS.client, protocol = securityConfig.protocol,
      hostname = securityConfig.hostname, port = securityConfig.port,
      creds = GestaltBasicCredentials(securityConfig.apiKey.getOrElse("anonymous"),securityConfig.apiSecret.getOrElse(""))
    )
}
