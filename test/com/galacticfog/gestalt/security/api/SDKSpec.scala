package com.galacticfog.gestalt.security.api

import mockws.{Route, MockWS}
import org.junit.runner._
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.http.{ContentTypeOf, Writeable}
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS, WSClient}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits, FakeApplication, WithApplication}
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class SDKSpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  trait TestParameters extends Scope {
    val hostname = "security.galacticfog.com"
    val port = 1234
    val apiKey = "apiKey"
    val apiSecret = "apiSecret"
    val baseUrl = s"http://${hostname}:${port}"
    def ws: MockWS
    def getSecurity = GestaltSecurityClient(ws,HTTP,hostname,port,apiKey,apiSecret)
  }

  "SDK" should {

    "return current org" in new TestParameters {
      val testResp = Json.obj(
        "orgName" -> "Test Org",
        "orgId" -> "abcdefgh"
      )
      val url = baseUrl + "/orgs/current"
      val ws = MockWS {
        case (GET, baseUrl) => Action { Ok(testResp) }
      }
      implicit val security = getSecurity
      val org = await(GestaltOrg.getCurrentOrg)
      org.orgName must_== (testResp \ "orgName").as[String]
      org.orgId must_== (testResp \ "orgId").as[String]
    }

    "handle leading slash and no leading slash" in new TestParameters {
      val base = Route {
        case (GET, baseUrl) => Action {
          Ok(Json.obj())
        }
      }
      val ws = MockWS(base)
      val security = getSecurity
      await(security.get("/")).toString() must_== "{}"
      await(security.get("")).toString() must_== "{}"
      base.timeCalled must_== 2
    }

    "list apps" in new TestParameters {
      val testOrg = GestaltOrg("ORGID","Test Name")
      val app1 = GestaltApp("APP1ID","App1",testOrg)
      val app2 = GestaltApp("APP2ID","App2",testOrg)
      val testResp = Json.toJson( Seq(app1,app2) )
      val url = baseUrl + s"/org/${testOrg.orgId}/apps"
      val ws = MockWS {
        case (GET, url) => Action { Ok(testResp) }
      }
      implicit val security = getSecurity
      val apps = await(GestaltOrg.getApps(testOrg))
      apps must haveSize(2)
      apps must contain(app1)
      apps must contain(app2)
    }

    "get an app by ID" in new TestParameters {
      val app1 = GestaltApp("APP1ID","Test App",GestaltOrg("ORGID","Test Org"))
      val url = baseUrl + s"/apps/${app1.appId}"
      val ws = MockWS {
        case (GET, url) => Action { Ok(Json.toJson(app1)) }
      }
      implicit val security = getSecurity
      val app = await(GestaltApp.getAppById(app1.appId))
      app must beSome(app1)
    }

    "handle missing app with None" in new TestParameters {
      val appid = "missing"
      val url = baseUrl + s"/apps/${appid}"
      val ws = MockWS {
        case (GET, url) => Action { NotFound("/apps/missing") }
      }
      implicit val security = getSecurity
      val app = await(GestaltApp.getAppById(appid))
      app must beNone
    }

    "authenticate a user" in new TestParameters {
      val app = GestaltApp("APPID","",GestaltOrg("ORGID",""))
      val account = GestaltAccount("jdoe", "John", "Doe", "jdoe@galacticfog.com")
      val grant = GestaltRightGrant("launcher:full_access",None)
      val authResponse = GestaltAuthResponse(account, Seq(grant))
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${app.appId}/auth"
      val ws = MockWS {
        case (POST, url) => Action(BodyParsers.parse.json) { request =>
          if (request.body.equals(creds.toJson)) Ok(Json.toJson(authResponse))
          else Forbidden("")
        }
      }
      implicit val security = getSecurity
      val testResponse = await(app.authorizeUser(creds))
      testResponse must beSome(authResponse)
    }

    "handle failed user authentication with a None" in new TestParameters {
      val app = GestaltApp("APPID","",GestaltOrg("ORGID",""))
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${app.appId}/auth"
      val ws = MockWS {
        case (POST, url) => Action {
          Forbidden("")
        }
      }
      implicit val security = getSecurity
      val testResponse = await(app.authorizeUser(creds))
      testResponse must beNone
    }

    "handle failed API authentication with an exception" in new TestParameters {
      val app = GestaltApp("APPID","",GestaltOrg("ORGID",""))
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${app.appId}/auth"
      val ws = MockWS {
        case (POST, url) => Action {
          Unauthorized("")
        }
      }
      implicit val security = getSecurity
      await(app.authorizeUser(creds)) must throwA[UnauthorizedAPIException]
    }

  }

}
