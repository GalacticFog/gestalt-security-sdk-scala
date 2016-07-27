package com.galacticfog.gestalt.security.api

import mockws.MockWS
import org.junit.runner._
import org.mockito.internal.matchers
import org.mockito.{ArgumentMatcher, Matchers}
import org.mockito.internal.matchers.VarargMatcher
import org.specs2.mock._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.http.{HeaderNames, ContentTypeOf, Writeable}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, FakeApplication, WithApplication}
import play.test.FakeRequest

import scala.collection.mutable
import scala.concurrent.Future

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class GestaltSecuritySpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  "GestaltSecurityClient" should {

    "provide a version" in {
      GestaltSecurityClient.getVersion must_== "2.2.7-SNAPSHOT"
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
      val testHolder = mock[WSRequestHolder]
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
      testHolder.post(Matchers.any[String])(Matchers.any[Writeable[String]], Matchers.any[ContentTypeOf[String]]) returns futureResponse
      testHolder.put(Matchers.any[String])(Matchers.any[Writeable[String]], Matchers.any[ContentTypeOf[String]]) returns futureResponse
      testHolder.patch(Matchers.any[String])(Matchers.any[Writeable[String]], Matchers.any[ContentTypeOf[String]]) returns futureResponse
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

}
