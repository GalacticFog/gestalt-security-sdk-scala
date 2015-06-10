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
      val url = baseUrl + s"/orgs/${testOrg.orgId}/apps"
      val ws = MockWS {
        case (GET, url) => Action { Ok(testResp) }
      }
      implicit val security = getSecurity
      val apps = await(GestaltOrg.getApps(testOrg))
      apps must haveSize(2)
      apps must contain(app1)
      apps must contain(app2)
    }

    "create user" in new TestParameters {
      val testApp = GestaltApp("APPID","AppName",GestaltOrg("ORGID","SomeOrg"))
      val returnedUser = GestaltAccount(
        username = "oldfart2",
        firstName = "John",
        lastName = "Perry",
        email = "jperry202@cdf.mil"
      )
      val createRequest = GestaltAccountCreate(
        username = returnedUser.username,
        firstName = returnedUser.firstName,
        lastName = returnedUser.lastName,
        email = returnedUser.email,
        credential = GestaltPasswordCredential("kathy"),
        rights = Some(Seq(
          GestaltRightGrant("strength", Some("11")),
          GestaltRightGrant("empee:28abj38dja", None)
        ))
      )
      val url = baseUrl + s"/apps/${testApp.appId}/users"
      val ws = MockWS {
        case (POST, url) => Action { request =>
          request.body.asJson match {
            case Some(js) =>
              // check parsing ability: gestalt-security uses this
              val c = js.as[GestaltAccountCreate]
              // can't compare the objects directly; GestaltPasswordCredential doesn't have a credentialType field
              // instead, re-serialize, check the JSON
              if (Json.toJson(c).equals(Json.toJson(createRequest))) Created(Json.toJson(returnedUser))
              else BadRequest("did not get the json body I was expecting")
            case None => BadRequest("was expecting json")
          }
        }
      }
      implicit val security = getSecurity
      val newUser = await(testApp.createUser(createRequest))
      newUser must beSuccessfulTry.withValue(returnedUser)
    }

    "create user failure returns failed try" in new TestParameters {
      val testApp = GestaltApp("APPID","AppName",GestaltOrg("ORGID","SomeOrg"))
      val createRequest = GestaltAccountCreate(
        username = "",
        firstName = "",
        lastName = "",
        email = "",
        credential = GestaltPasswordCredential(""),
        rights = None
      )
      val url = baseUrl + s"/apps/${testApp.appId}/users"
      val ws = MockWS {
        case (POST, url) => Action { BadRequest(Json.toJson(BadRequestException("username","some message","some developer message"))) }
      }
      implicit val security = getSecurity
      val newUser = await(testApp.createUser(createRequest))
      newUser must beFailedTry.withThrowable[BadRequestException]
    }

    "add a right grant" in new TestParameters {
      val testApp = GestaltApp("APPID","AppName",GestaltOrg("ORGID","SomeOrg"))
      val testUsername = "someUsersName"
      val createGrant = GestaltRightGrant("testGrantName", Some("testGrantValue"))
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights/${createGrant.grantName}"
      val ws = MockWS {
        case (POST, url) => Action { Ok(Json.toJson(createGrant)) }
      }
      implicit val security = getSecurity
      val newUser = await(testApp.addGrant(testUsername,createGrant))
      newUser must beSuccessfulTry(createGrant)
    }

    "list right grants" in new TestParameters {
      val testApp = GestaltApp("APPID","Test Name",GestaltOrg("ORGID","OrgName"))
      val testUsername = "someUsersName"
      val grant1 = GestaltRightGrant("grant1",None)
      val grant2 = GestaltRightGrant("grant2",Some("value2"))
      val testResp = Json.toJson( Seq(grant1,grant2) )
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights"
      val ws = MockWS {
        case (GET, url) => Action { Ok(testResp) }
      }
      implicit val security = getSecurity
      val apps = await(testApp.listGrants(testUsername))
      apps must beSuccessfulTry(Seq(grant1,grant2))
    }

    "list right grants for 404 returns failure" in new TestParameters {
      val testApp = GestaltApp("APPID","Test Name",GestaltOrg("ORGID","OrgName"))
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights"
      val ws = MockWS {
        case (GET, url) => Action { NotFound(Json.toJson(ResourceNotFoundException("username","resource missing","I have no idea what you're asking for."))) }
      }
      implicit val security = getSecurity
      val apps = await(testApp.listGrants(testUsername))
      apps must beFailedTry.withThrowable[ResourceNotFoundException]
    }

    "list right grants for 400 returns failure" in new TestParameters {
      val testApp = GestaltApp("APPID","Test Name",GestaltOrg("ORGID","OrgName"))
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights"
      val ws = MockWS {
        case (GET, url) => Action { BadRequest(Json.toJson(BadRequestException("username","you did something bad","You've probably done something bad."))) }
      }
      implicit val security = getSecurity
      val apps = await(testApp.listGrants(testUsername))
      apps must beFailedTry.withThrowable[BadRequestException]
    }

    "delete extant right grant" in new TestParameters {
      val testApp = GestaltApp("APPID","Test Name",GestaltOrg("ORGID","OrgName"))
      val testUsername = "someUsersName"
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights/${testGrantName}"
      val ws = MockWS {
        case (DELETE, url) => Action { Ok(Json.toJson(DeleteResult(true))) }
      }
      implicit val security = getSecurity
      val apps = await(testApp.deleteGrant(testUsername,testGrantName))
      apps must beSuccessfulTry(true)
    }.pendingUntilFixed("something is wrong with MockWS handling of DELETE")

    "delete nonexistant right grant" in new TestParameters {
      val testApp = GestaltApp("APPID","Test Name",GestaltOrg("ORGID","OrgName"))
      val testUsername = "someUsersName"
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.appId}/users/${testUsername}/rights/${testGrantName}"
      val ws = MockWS {
        case (DELETE, url) => Action { Ok(Json.toJson(DeleteResult(true))) }
      }
      implicit val security = getSecurity
      val apps = await(testApp.deleteGrant(testUsername,testGrantName))
      apps must beSuccessfulTry(false)
    }.pendingUntilFixed("something is wrong with MockWS handling of DELETE")

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
        case (GET, url) => Action { NotFound(Json.toJson(ResourceNotFoundException("appId","app not found","Application with specified ID does not exist. Make sure you are using an application ID and not the application name."))) }
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
          else Forbidden(Json.toJson(ForbiddenAPIException("failed to auth","failed to auth")))
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
          Forbidden(Json.toJson(ForbiddenAPIException("account authentication failed","Authentication of application account failed due to invalid account credentials.")))
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
          Unauthorized(Json.toJson(UnauthorizedAPIException("API authentication failed","Authentication of API credentials failed.")))
        }
      }
      implicit val security = getSecurity
      await(app.authorizeUser(creds)) must throwA[UnauthorizedAPIException]
    }

    "properly produce and parse JSON for UnknownAPIException objects" in {
      val ex = UnknownAPIException(500,"res","foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "properly produce and parse JSON for BadRequestException objects" in {
      val ex = BadRequestException("res","foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "properly produce and parse JSON for CreateConflictException objects" in {
      val ex = CreateConflictException("res","foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "properly produce and parse JSON for ForbiddenAPIException objects" in {
      val ex = ForbiddenAPIException("foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "properly produce and parse JSON for ResourceNotFoundException objects" in {
      val ex = ResourceNotFoundException("res","foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "properly produce and parse JSON for UnauthorizedAPIException objects" in {
      val ex = UnauthorizedAPIException("foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

  }

}
