package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.sdk.BuildInfo
import mockws.MockWS
import org.junit.runner._
import org.mockito.internal.matchers
import org.mockito.{ArgumentMatcher, Matchers}
import org.mockito.internal.matchers.VarargMatcher
import org.specs2.matcher.{Expectable, MatchResult, Matcher}
import org.specs2.mock._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.http.{ContentTypeOf, HeaderNames, Writeable}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws._
import play.api.test.{DefaultAwaitTimeout, FakeApplication, FutureAwaits, WithApplication}
import play.test.FakeRequest

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class GestaltSecuritySpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  "GestaltSecurityClient" should {

    "provide a version" in {
      GestaltSecurityClient.getVersion must_== BuildInfo.version
    }

    "provide a sha" in {
      GestaltSecurityClient.getSHA must not beEmpty
    }

    "accept and return provided wsclient" in {
      val mockWS = mock[WSClient]
      val security = GestaltSecurityClient(mockWS,HTTP,"localhost",9000,"someKey","someSecret")
      security.client == mockWS
    }

    "use application wsclient by default" in new WithApplication {
      val security = GestaltSecurityClient(HTTP,"localhost",9000,"someKey","someSecret")
      security.client == WS.client
    }

    "allow credentials override" in new WithApplication {
      val newCreds = GestaltBasicCredentials("otherKey", "otherSecret")
      val security = GestaltSecurityClient(HTTP,"localhost",9000,"someKey","someSecret").withCreds(newCreds)
      security.creds must beAnInstanceOf[GestaltBasicCredentials]
      security.creds.asInstanceOf[GestaltBasicCredentials] must_== newCreds
    }

    class FullyMockedWSClient extends Scope {
      val wsclient = mock[WSClient]
      val testHolder = mock[WSRequest]

      testHolder.withHeaders(any) returns testHolder
      testHolder.withAuth(any,any,any) returns testHolder
      testHolder.withQueryString(any) returns testHolder
      wsclient.url(anyString) returns testHolder
      
      val response = mock[WSResponse]
      val futureResponse = Future{response}
      
      response.status returns 200
      response.statusText returns "Ok"
      response.body returns ""
      response.json returns Json.obj()
      
      testHolder.get returns futureResponse
      
      testHolder.post(anyString)(any[Writeable[String]]) returns Future.successful(response)
      testHolder.put(Matchers.any[String])(any[Writeable[String]]) returns Future.successful(response)  // returns futureResponse//(Matchers.any[Writeable[String]], Matchers.any[ContentTypeOf[String]]) returns futureResponse
      testHolder.patch(Matchers.any[String])(any[Writeable[String]]) returns Future.successful(response) // returns futureResponse//(Matchers.any[Writeable[String]], Matchers.any[ContentTypeOf[String]]) returns futureResponse
      testHolder.delete returns futureResponse

      val hostname = "localhost"
      val port = 1234
      val apiKey = "someKey"
      val apiSecret = "someSecret"
      val someToken = "some.token"
      val basicCreds = GestaltBasicCredentials(apiKey,apiSecret)
      val tokenCreds = GestaltBearerCredentials(someToken)
      val security = GestaltSecurityClient(wsclient,HTTP,hostname,port,apiKey,apiSecret)
    }

    class HadHeader extends ArgumentMatcher[mutable.WrappedArray[(String,String)]] {
      override def matches(argument: scala.Any): Boolean = ???
    }

    "properly use apiKey and apiSecret for authentication on GET" in new FullyMockedWSClient {
      await(security.getJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use apiKey and apiSecret for authentication on DELETE" in new FullyMockedWSClient {
      await(security.deleteJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use apiKey and apiSecret for authentication on POST(empty)" in new FullyMockedWSClient {
      await(security.postJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use apiKey and apiSecret for authentication on POST(body)" in new FullyMockedWSClient {
      await(security.postJson("/", Json.obj()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use apiKey and apiSecret for authentication on POST(form)" in new FullyMockedWSClient {
      await(security.postForm[JsValue]("/", Map()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "not use apiKey and apiSecret for authentication on POSTNoAuth(form)" in new FullyMockedWSClient {
      await(security.postFormNoAuth[JsValue]("/", Map()))
      there were no(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use apiKey and apiSecret for authentication on PATCH" in new FullyMockedWSClient {
      await(security.patchJson("/", Json.obj()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> basicCreds.headerValue)
    }

    "properly use token for authentication on GET" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).getJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "properly use token for authentication on DELETE" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).deleteJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "properly use token for authentication on POST(empty)" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).postJson("/"))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "properly use token for authentication on POST(body)" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).postJson("/",Json.obj()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "properly use token for authentication on POST(form)" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).postForm[JsValue]("/", Map()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "not use token for authentication on POSTNoAuth(form)" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).postFormNoAuth[JsValue]("/", Map()))
      there were no(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "properly use token for authentication on PATCH" in new FullyMockedWSClient {
      await(security.withCreds(tokenCreds).patchJson("/",Json.obj()))
      there was one(testHolder).withHeaders(HeaderNames.AUTHORIZATION -> tokenCreds.headerValue)
    }

    "consistently encode and decode Basic authorization" in {
      val creds = GestaltBasicCredentials("root", "letmein")
      GestaltAPICredentials.getCredentials(
        creds.headerValue
      ) must beSome(creds)
      GestaltAPICredentials.getCredentials(
        creds.headerValue + " Realm(test.com)"
      ) must beSome(creds)
    }

    "consistently encode and decode Bearer authorization" in {
      val creds = GestaltBearerCredentials("some_token")
      val authHeader = creds.headerValue
      GestaltAPICredentials.getCredentials(authHeader) must beSome(creds)
    }

    "decode dcos token authentication" in {
      val creds = GestaltBearerCredentials("some_token")
      val authHeader = "token=" + creds.token
      GestaltAPICredentials.getCredentials(authHeader) must beSome(creds)
    }

    "return null on improperly formatted auth headers" in {
      GestaltAPICredentials.getCredentials("badstring") must beNone
    }

  }

  "GestaltSecurityConfig" should {

    import GestaltSecurityConfig.ePROTOCOL
    import GestaltSecurityConfig.eHOSTNAME
    import GestaltSecurityConfig.ePORT
    import GestaltSecurityConfig.eKEY
    import GestaltSecurityConfig.eSECRET
    import GestaltSecurityConfig.eAPPID

    val envDelegated = Map(
       ePROTOCOL -> "HTTP",
       eHOSTNAME -> "hostname",
       ePORT     -> "9455",
       eKEY      -> UUID.randomUUID().toString,
       eSECRET   -> "secret",
       eAPPID    -> UUID.randomUUID().toString
    )

    def withMode(mode: => GestaltSecurityMode) = ((_:GestaltSecurityConfig).mode) ^^ be_==(mode)
    def beWellDefined = ((_:GestaltSecurityConfig).isWellDefined)
    def withPort(port: => Int) = ((_:GestaltSecurityConfig).port) ^^ be_==(port)

    "return None if env vars are missing on FromEnv" in {
      GestaltSecurityConfig.getSecurityConfigFromEnv must beNone
    }

    "configure in delegated mode if appId is present" in {
      GestaltSecurityConfig.getSecurityConfigFromEnv(envDelegated.get) must beSome(withMode(DELEGATED_SECURITY_MODE) and withPort(9455) and beWellDefined)
    }

    "configure in framework mode if appId is not present" in {
      GestaltSecurityConfig.getSecurityConfigFromEnv( (envDelegated - GestaltSecurityConfig.eAPPID).get) must beSome(withMode(FRAMEWORK_SECURITY_MODE) and withPort(9455) and beWellDefined)
    }

    "use protocol defined default port" in {
      GestaltSecurityConfig.getSecurityConfigFromEnv(
        (envDelegated - ePORT + (ePROTOCOL -> "HTTP")).get
      ) must beSome(withMode(DELEGATED_SECURITY_MODE) and withPort(80) and beWellDefined)
      GestaltSecurityConfig.getSecurityConfigFromEnv(
        (envDelegated - ePORT + (ePROTOCOL -> "HTTPS")).get
      ) must beSome(withMode(DELEGATED_SECURITY_MODE) and withPort(443) and beWellDefined)
      GestaltSecurityConfig.getSecurityConfigFromEnv(
        (envDelegated - eAPPID - ePORT + (ePROTOCOL -> "HTTP")).get
      ) must beSome(withMode(FRAMEWORK_SECURITY_MODE) and withPort(80) and beWellDefined)
      GestaltSecurityConfig.getSecurityConfigFromEnv(
        (envDelegated - eAPPID - ePORT + (ePROTOCOL -> "HTTPS")).get
      ) must beSome(withMode(FRAMEWORK_SECURITY_MODE) and withPort(443) and beWellDefined)
    }

  }

}
