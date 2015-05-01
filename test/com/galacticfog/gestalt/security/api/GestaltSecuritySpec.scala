package com.galacticfog.gestalt.security.api

import org.junit.runner._
import org.mockito.Matchers
import org.specs2.mock._
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.http.{ContentTypeOf, Writeable}
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, FakeApplication, WithApplication}

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
      val security = GestaltSecurityClient(wsclient,HTTP,hostname,port,apiKey,apiSecret)
    }

    "use apiKey and apiSecret for authentication on GET" in new FullyMockedWSClient {
      await(security.get("/"))
      there was one(testHolder).withAuth(Matchers.eq(apiKey), Matchers.eq(apiSecret), Matchers.any[WSAuthScheme])
    }

    "use apiKey and apiSecret for authentication on DELETE" in new FullyMockedWSClient {
      await(security.delete("/"))
      there was one(testHolder).withAuth(Matchers.eq(apiKey), Matchers.eq(apiSecret), Matchers.any[WSAuthScheme])
    }

    "use apiKey and apiSecret for authentication on POST" in new FullyMockedWSClient {
      await(security.post("/"))
      there was one(testHolder).withAuth(Matchers.eq(apiKey), Matchers.eq(apiSecret), Matchers.any[WSAuthScheme])
    }

    "use apiKey and apiSecret for authentication on POST(body)" in new FullyMockedWSClient {
      await(security.post("/",Json.obj()))
      there was one(testHolder).withAuth(Matchers.eq(apiKey), Matchers.eq(apiSecret), Matchers.any[WSAuthScheme])
    }
  }

}
