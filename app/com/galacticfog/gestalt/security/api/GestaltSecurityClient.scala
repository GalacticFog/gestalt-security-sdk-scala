package com.galacticfog.gestalt.security.api

import com.galacticfog.gestalt.security.api.errors._
import com.galacticfog.gestalt.security.sdk.BuildInfo
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

  def withCreds(creds: GestaltAPICredentials) = new GestaltSecurityClient(
    client = this.client,
    protocol = this.protocol,
    hostname = this.hostname,
    port = this.port,
    creds = creds
  )

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
    patchJson(uri, payload) map validate[T]
  }

  def post[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri, payload) map validate[T]
  }

  def postEmpty[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    postJson(uri) map validate[T]
  }

  def postFormNoAuth[T](endpoint: String, fields: Map[String, String])(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    client
      .url(genUri(endpoint))
      .post(fields.mapValues(Seq(_))) flatMap processResponse map validate[T]
  }

  def postForm[T](endpoint: String, fields: Map[String, String])(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    addAuth(client.url(genUri(endpoint)))
      .post(fields.mapValues(Seq(_))) flatMap processResponse map validate[T]
  }

  def put[T](uri: String, payload: JsValue)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    putJson(uri,payload) map validate[T]
  }

  def getQuery[T](uri: String, qs: (String,String)*)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri,qs:_*) map validate[T]
  }

  def get[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    getJson(uri) map validate[T]
  }

  def deleteDR(uri: String): Future[DeleteResult] = {
    deleteJson(uri) map validate[DeleteResult]
  }

  def delete[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[T] = {
    deleteJson(uri) map validate[T]
  }

  def getOpt[T](uri: String)(implicit fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]): Future[Option[T]] = {
    getJson(uri) map {
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
          case Success(json) => json.asOpt[OAuthError] orElse json.asOpt[SecurityRESTException] match {
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

  private def addAuth(rh: WSRequestHolder) = rh.withHeaders(HeaderNames.AUTHORIZATION -> this.creds.headerValue)

  private def genRequest(sendingJson: Boolean, endpoint: String): WSRequestHolder = {
    val rh = addAuth( client.url(genUri(endpoint)) )
    if (sendingJson) rh.withHeaders(
        "Content-Type" -> MimeTypes.JSON,
        "Accept" -> MimeTypes.JSON
      )
    else rh.withHeaders("Accept" -> MimeTypes.JSON)
  }

  private def genUri(endpoint: String): String = {
    s"${protocol}://${hostname}:${port}/${removeLeadingSlash(endpoint)}"
  }

  def getJson(endpoint: String, qs: (String,String)*): Future[JsValue] =
    genRequest(sendingJson = false, endpoint)
      .withQueryString(qs:_*)
      .get() flatMap processResponse

  def postJson(endpoint: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint).post("") flatMap processResponse

  def postJson(endpoint: String, payload: JsValue): Future[JsValue] =
    genRequest(sendingJson = true, endpoint).post(payload) flatMap processResponse

  def patchJson(endpoint: String, payload: JsValue): Future[JsValue] =
    genRequest(sendingJson = true, endpoint).patch(payload) flatMap processResponse

  def putJson(endpoint: String, payload: JsValue): Future[JsValue] =
    genRequest(sendingJson = true, endpoint).put(payload) flatMap processResponse

  def deleteJson(endpoint: String): Future[JsValue] =
    genRequest(sendingJson = false, endpoint).delete() flatMap processResponse
}

object GestaltSecurityClient {

  def getVersion: String = BuildInfo.version

  def getSHA: String = BuildInfo.gitHash

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
      creds = GestaltBasicCredentials(securityConfig.apiKey,securityConfig.apiSecret)
    )
}
