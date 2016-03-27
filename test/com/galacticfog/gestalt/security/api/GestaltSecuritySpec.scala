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
import play.api.libs.json.Json
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

  "GestaltSecurity object" should {

    "accept and return provided wsclient" in {
      val mockWS = mock[WSClient]
      val security = GestaltSecurityClient(mockWS,HTTP,"localhost",9000,"someKey","someSecret")
      security.client == mockWS
    }

    "use application wsclient by default" in new WithApplication {
      val security = GestaltSecurityClient(HTTP,"localhost",9000,"someKey","someSecret")
      security.client == WS.client
    }

    class FullyMockedWSClient extends Scope {
      val wsclient = mock[WSClient]
      val testHolder = mock[WSRequestHolder]
      testHolder.withHeaders(any) returns testHolder
      testHolder.withAuth(any,any,any) returns testHolder
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
      val basicCreds = GestaltBasicCredentials(apiKey,apiSecret)
      val security = GestaltSecurityClient(wsclient,HTTP,hostname,port,apiKey,apiSecret)
    }

    class HadHeader extends ArgumentMatcher[mutable.WrappedArray[(String,String)]] {
      override def matches(argument: scala.Any): Boolean = ???
    }

    "use apiKey and apiSecret for authentication on GET" in new FullyMockedWSClient {
      await(security.getJson("/",basicCreds))
//      there was one(testHolder).withAuth(Matchers.eq(apiKey), Matchers.eq(apiSecret), Matchers.any[WSAuthScheme])
      there was one(testHolder).withHeaders("Basic" -> basicCreds.headerValue)
    }

    "use apiKey and apiSecret for authentication on DELETE" in new FullyMockedWSClient {
      await(security.deleteJson("/",basicCreds))
      there was one(testHolder).withHeaders("Basic" -> basicCreds.headerValue)
    }

    "use apiKey and apiSecret for authentication on POST(empty)" in new FullyMockedWSClient {
      await(security.postJson("/",basicCreds))
      there was one(testHolder).withHeaders("Basic" -> basicCreds.headerValue)
    }

    "use apiKey and apiSecret for authentication on POST(body)" in new FullyMockedWSClient {
      await(security.postJson("/",Json.obj(),basicCreds))
      there was one(testHolder).withHeaders("Basic" -> basicCreds.headerValue)
    }

    "consistently encode and decode Basic authorization" in {
      val creds = GestaltBasicCredentials("root", "letmein")
      GestaltAPICredentials.getCredentials(
        "Basic " + creds.headerValue
      ) must beSome(creds)
      GestaltAPICredentials.getCredentials(
        "Basic " + creds.headerValue + " Realm(test.com)"
      ) must beSome(creds)
    }

    "consistently encode and decode Bearer authorization" in {
      val creds = GestaltBearerCredentials("some_token")
      val authHeader = "Bearer " + creds.headerValue
      GestaltAPICredentials.getCredentials(authHeader) must beSome(creds)
    }

    "return null on improperly formatted auth headers" in {
      GestaltAPICredentials.getCredentials("badstring") must beNone
    }

  }

}
