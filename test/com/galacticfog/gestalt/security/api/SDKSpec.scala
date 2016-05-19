package com.galacticfog.gestalt.security.api

import java.time.Instant
import java.util.{UUID}

import com.galacticfog.gestalt.io.util.{PatchUpdate, PatchOp}
import com.galacticfog.gestalt.security.api.AccessTokenResponse.BEARER
import com.galacticfog.gestalt.security.api.GestaltToken.ACCESS_TOKEN
import org.joda.time.DateTime
import PatchUpdate._
import com.galacticfog.gestalt.security.api.errors._
import mockws.MockWS
import org.junit.runner._
import org.mockito.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._
import com.galacticfog.gestalt.security.api.json.JsonImports._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.annotation.meta.field
import scala.concurrent.Future
import scala.concurrent.duration._


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class SDKSpec extends Specification with Mockito with FutureAwaits with DefaultAwaitTimeout {

  case class TestClass(override val name: String,
                       val reqString: String,
                       val maybeInt: Option[Int] = None,
                       val maybeString: Option[String] = None) extends GestaltResource with PatchSupport[TestClass] {

    override val id: UUID = UUID.randomUUID()
    override val href: String = s"/tests/${id}"
    override val description = None
  }

  implicit val testFormat = Json.format[TestClass]

  trait TestParameters extends Scope {
    val hostname = "security.galacticfog.com"
    val port = 1234
    val apiKey = "apiKey"
    val apiSecret = "apiSecret"
    val baseUrl = s"http://${hostname}:${port}"

    val testOrg = GestaltOrg(UUID.randomUUID,"cdf.cu","cdf.cu",None,None,Seq())
    val testDir = GestaltDirectory(UUID.randomUUID,"Staff",Some("CDF staff"),testOrg.id)
    val testApp = GestaltApp(UUID.randomUUID,"inbox.cdf.cu",None,testOrg.id,false)
    val testAccount = GestaltAccount(
      id = UUID.randomUUID,
      username = "oldfart1",
      firstName = "John",
      lastName = "Perry",
      None,
      email = "jperry202@cdf.cu",
      phoneNumber = "850-867-5309",
      directory = testDir
    )
    val testMapping = GestaltAccountStoreMapping(UUID.randomUUID,"Staff Members",Some("Staff members authorized for this app."),DIRECTORY,testDir.id,testApp.id,false,false)

    val testToken = OpaqueToken(UUID.randomUUID(),ACCESS_TOKEN)
    val testValidTokenResponse = Json.obj(
      "username" -> s"${testAccount.username}",
      "sub" -> s"${testAccount.href}",
      "iss" -> "https://security:9455",
      "exp" -> Instant.now.getEpochSecond,
      "iat" -> Instant.now.getEpochSecond,
      "jti" -> testToken.id,
      "gestalt_token_href" -> testToken.href,
      "gestalt_rights" -> Json.arr(),
      "gestalt_groups" -> Json.arr(),
      "gestalt_account" -> Json.toJson(testAccount),
      "gestalt_org_id" -> testOrg.id.toString
    ).as[ValidTokenResponse]

    val testTokenAuthResponse = Json.obj(
      "access_token" -> testToken.toString,
      "token_type" -> BEARER.toString(),
      "expires_in" -> 3600,
      "gestalt_access_token_href" -> s"https://security:9455/tokens/${testToken.id.toString}"
    ).as[AccessTokenResponse]

    val testAPIKey = "jdoe"
    val testAPISecret = "monkey"
    val testDefaultCreds = GestaltBasicCredentials(testAPIKey, testAPISecret)
    val testOverrideCreds = GestaltBasicCredentials("different-username", "different-password")

    def getMockSecurity: GestaltSecurityClient = {
      val client = mock[GestaltSecurityClient]
      client.creds returns testDefaultCreds
      client
    }

    def getSecurity(routes: Tuple3[String,String,Action[AnyContent]]): GestaltSecurityClient = {
      val r = routes
      val mockws = MockWS {
        case (m,u) if m == r._1 && u == r._2 => r._3
      }
      GestaltSecurityClient(mockws, HTTP, hostname, port, apiKey, apiSecret)
    }

    def getSecurityJson(routes: Tuple3[String,String,Action[JsValue]]): GestaltSecurityClient = {
      val r = routes
      val mockws = MockWS {
        case (m,u) if m == r._1 && u == r._2 => r._3
      }
      GestaltSecurityClient(mockws, HTTP, hostname, port, apiKey, apiSecret)
    }

    def mockGet[T](uri: String, maybeCreds: Option[GestaltAPICredentials], ret: T)(implicit client: GestaltSecurityClient, fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]) = {
      client.get[T](
        Matchers.eq(uri)
      )(
        Matchers.any[Reads[T]], Matchers.eq(m)
      ) returns Future.successful(ret)
    }

    def mockPatch[T](uri: String, payload: JsValue, maybeCreds: Option[GestaltAPICredentials], ret: T)(implicit client: GestaltSecurityClient, fjs : play.api.libs.json.Reads[T], m: reflect.Manifest[T]) = {
      client.patch[T](
        Matchers.eq(uri), Matchers.eq(payload)
      )(
        Matchers.any[Reads[T]], Matchers.eq(m)
      ) returns Future.successful(ret)
    }

  }

  "GestaltResource" should {

    "support patch add on option fields" in {
      val test = TestClass(
        name = "oldName",
        maybeInt = Some(1),
        reqString = "value"
      )

      val patches = PatchSupport.genPatch(test,
        'name -> Json.toJson("newName"),
        'maybeString -> Json.toJson("string")
      )
      patches must containTheSameElementsAs(Seq(
        PatchOp("replace", "/name",        JsString("newName")),
        PatchOp("add",     "/maybeString", JsString("string"))
      ))
    }

    "support patch replace on option fields" in {
      val test = TestClass(
        name = "oldName",
        reqString = "value",
        maybeInt = Some(1)
      )

      val patches = PatchSupport.genPatch(test,
        'maybeInt -> Json.toJson(1)
      )
      patches must containTheSameElementsAs(Seq(
        PatchOp("replace", "/maybeInt", Json.toJson(1))
      ))
    }

    "support patch remove" in {
      val test = TestClass(
        name = "name",
        reqString = "value",
        maybeInt = Some(1),
        maybeString = Some("string")
      )
      val patches = PatchSupport.genPatch(test,
        'maybeInt -> PatchSupport.REMOVE,
        'maybeString -> PatchSupport.REMOVE
      )
      patches must containTheSameElementsAs(Seq(
        PatchOp("remove", "/maybeInt", JsString("")),
        PatchOp("remove", "/maybeString", JsString(""))
      ))
    }

    "throw an error on invalid fields" in {
      val test = TestClass("name", "value")
      PatchSupport.genPatch(test,
        'maybeInt -> PatchSupport.REMOVE,
        'badField -> Json.toJson("no matter")
      ) must throwA[RuntimeException](".*does not exist.*")
    }

    "throw an exception if REMOVE passed to a non-Option field" in {
      val test = TestClass("name", "value")
      PatchSupport.genPatch(test,
        'name -> PatchSupport.REMOVE
      ) must throwA[RuntimeException](".*is not Option.*")
    }

    "throw an exception on repeated fields" in {
      val test = TestClass("name", "value")
      PatchSupport.genPatch(test,
        'maybeInt -> PatchSupport.REMOVE,
        'maybeInt -> Json.toJson(5)
      ) must throwA[RuntimeException](".*multiple times.*")
    }

    "patch appropriately via the client" in new TestParameters {
      val test = TestClass(name = "name", reqString = "value", maybeInt = Some(1), maybeString = None)
      val expected = TestClass(name = "newName", reqString = "newValue", maybeInt = None, maybeString = Some("present"))

      val route = ("PATCH", baseUrl + test.href, Action(BodyParsers.parse.json) { implicit request =>
        val patches = request.body.as[Seq[PatchOp]]
        val patched = patches.foldLeft(test)( (c,p) =>
          p.path match {
            case "/name" => if (p.op == "replace") {
              c.copy(name = p.value.as[String])
            } else throw new RuntimeException
            case "/reqString" => if (p.op == "replace") {
              c.copy(reqString = p.value.as[String])
            } else throw new RuntimeException
            case "/maybeInt" => p.op match {
              case "replace" if c.maybeInt.isDefined => c.copy(maybeInt = Some(p.value.as[Int]))
              case "add" if c.maybeInt.isEmpty => c.copy(maybeInt = Some(p.value.as[Int]))
              case "remove" => c.copy(maybeInt = None)
              case _ => throw new RuntimeException
            }
            case "/maybeString" => p.op match {
              case "replace" if c.maybeString.isDefined => c.copy(maybeString = Some(p.value.as[String]))
              case "add" if c.maybeString.isEmpty => c.copy(maybeString = Some(p.value.as[String]))
              case "remove" => c.copy(maybeString = None)
              case _ => throw new RuntimeException
            }
          }
        )
        Ok(Json.toJson(patched))
      })

      implicit val client = getSecurityJson(route)

      val updated: TestClass = await(test.update(
        'name -> Json.toJson("newName"),
        'reqString -> Json.toJson("newValue"),
        'maybeInt -> PatchSupport.REMOVE,
        'maybeString -> Json.toJson("present")
      ))

      updated must_== expected
    }

  }

  "SDK client" should {

    "handle leading slash and no leading slash" in new TestParameters {
      val route = (GET, baseUrl + "/something", Action{Ok(Json.obj())} )
      val security = getSecurity(route)
      await(security.getJson("/something")).toString() must_== "{}"
      await(security.getJson("something")).toString() must_== "{}"
    }

    "handle failed API authentication with an exception" in new TestParameters {
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST,url, Action {
          Unauthorized(Json.toJson(UnauthorizedAPIException("resource", "API authentication failed","Authentication of API credentials failed.")))
        })
      implicit val security = getSecurity(route)
      await(testApp.authorizeUser(creds)) must throwA[UnauthorizedAPIException]
    }

    "properly fail on NoContent responses" in new TestParameters {
      val url = baseUrl + "/dummy"
      val route = (GET,url, Action { NoContent })
      val security = getSecurity(route)
      val jsonTry = security.getJson("dummy")
      await(jsonTry) must throwAn[APIParseException]
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
      val ex = ConflictException("res","foo","bar")
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
      val ex = UnauthorizedAPIException("resource", "foo","bar")
      val json = Json.toJson(ex)
      val ex2 = json.as[SecurityRESTException]
      ex2 must_== ex
    }

    "neglect body and Content-Type on postTry(empty)" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (POST, url, Action {
        implicit request =>
          if (request.body.asText.exists(_.isEmpty) && request.body.asJson.isEmpty) Ok(Json.toJson(DeleteResult(true)))
          else BadRequest("")
      })
      val security = getSecurity(route)
      val deleted = await(security.postEmpty[DeleteResult]("something"))
      deleted must_== DeleteResult(true)
    }

    "returns UnknownAPIException on weird JSON error responses" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (GET, url, Action {
        BadRequest(Json.obj(
          "me" -> "not a SecurityRESTException"
        ))
      })
      val security = getSecurity(route)
      await(security.get[GestaltOrg]("something")) must throwA[UnknownAPIException](".*could not parse to SecurityRESTException.*")
    }

    "returns UnknownAPIException on non-JSON error responses" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (GET, url, Action {
        BadRequest("Not JSON")
      })
      val security = getSecurity(route)
      await(security.get[GestaltOrg]("something")) must throwA[UnknownAPIException](".*could not parse to JSON.*")
    }

  }

  "GestaltOrg" should {

    "return a sane href" in new TestParameters {
      testOrg.href must_== s"/orgs/${testOrg.id}"
    }

    "support sync against org root" in new TestParameters {
      val chld = GestaltOrg(UUID.randomUUID(), "child", "child", None, None, Seq())
      val root = GestaltOrg(UUID.randomUUID(), "root", "root", None, None, Seq(chld.getLink))
      val jane = GestaltAccount(UUID.randomUUID(), username = "jdee", "Jane", "Dee", None, "jdee@org", "", testDir)
      val john = GestaltAccount(UUID.randomUUID(), username = "jdoe", "John", "Doe", None, "jdoe@chld.org", "", testDir)
      val awayTeam = GestaltGroup(UUID.randomUUID(), "away-team", None, testDir, false, accounts = Seq(john.getLink(), jane.getLink()))
      val rootUrl = baseUrl + "/sync"
      val route = (GET, rootUrl, Action {
        Ok(Json.toJson(GestaltOrgSync(
          accounts = Seq( jane, john ),
          groups = Seq( awayTeam ),
          orgs = Seq(root,chld)
        )))
      })
      implicit val security = getSecurity(route)
      val rootSync = await(GestaltOrg.syncOrgTree(None))
      rootSync.orgs must containAllOf(Seq(root,chld))
      rootSync.accounts must containAllOf(Seq(jane,john))
      rootSync.groups must containAllOf(Seq(awayTeam))
      rootSync.groups(0).accounts.map({_.id}) must containAllOf(Seq(jane.id, john.id))
    }

    "support sync against suborg" in new TestParameters {
      val chld = GestaltOrg(UUID.randomUUID(), "child", "child", None, None, Seq())
      val jane = GestaltAccount(UUID.randomUUID(), username = "jdee", "Jane", "Dee", None, "jdee@org", "", testDir)
      val john = GestaltAccount(UUID.randomUUID(), username = "jdoe", "John", "Doe", None, "jdoe@chld.org", "", testDir)
      val awayTeam = GestaltGroup(UUID.randomUUID(), "away-team", None, testDir, false, accounts = Seq(john.getLink(), jane.getLink()))
      val chldUrl = baseUrl + s"/orgs/${chld.id}/sync"
      val route = (GET, chldUrl, Action {
        Ok(Json.toJson(GestaltOrgSync(
          accounts = Seq(jane,john),
          groups = Seq(awayTeam),
          orgs = Seq(chld)
        )))
      })
      implicit val security = getSecurity(route)
      val subSync = await(GestaltOrg.syncOrgTree(Some(chld.id)))
      subSync.orgs must_== Seq(chld)
      subSync.accounts must containAllOf(Seq(jane,john))
      subSync.groups must containAllOf(Seq(awayTeam))
      subSync.groups(0).accounts.map({_.id}) must containAllOf(Seq(jane.id, john.id))
    }

    "return current org" in new TestParameters {
      val returnedOrg = GestaltOrg(id = UUID.randomUUID, "Test Org", "abcdefgh", None, None, Seq() )
      val url = baseUrl + "/orgs/current"
      val route = (GET, url, Action { Ok(Json.toJson(returnedOrg)) })
      implicit val security = getSecurity(route)
      val org = await(GestaltOrg.getCurrentOrg())
      org must_== returnedOrg
    }

    "generate api credentials with no org" in new TestParameters {
      val url = baseUrl + s"/accounts/${testAccount.id}/apiKeys"
      val returnedAPIKey = GestaltAPIKey(
        apiKey = UUID.randomUUID().toString,
        apiSecret = Some("reallylongpassword"),
        accountId = testAccount.id,
        orgId = None,
        disabled = false
      )
      val route = (POST, url, Action(BodyParsers.parse.json) { request =>
        (request.body \ "orgId").asOpt[UUID] match {
          case None => Ok(Json.toJson(returnedAPIKey))
          case _ => BadRequest(Json.obj())
        }
      })
      implicit val security = getSecurityJson(route)
      val key = await(testAccount.generateAPICredentials())
      key must_== returnedAPIKey
    }

    "generate api credentials with org" in new TestParameters {
      val url = baseUrl + s"/accounts/${testAccount.id}/apiKeys"
      val returnedAPIKey = GestaltAPIKey(
        apiKey = UUID.randomUUID().toString,
        apiSecret = Some("reallylongpassword"),
        accountId = testAccount.id,
        orgId = Some(testOrg.id),
        disabled = false
      )
      val route = (POST, url, Action(BodyParsers.parse.json) { request =>
        (request.body \ "orgId").asOpt[UUID] match {
          case Some(testOrg.id) => Ok(Json.toJson(returnedAPIKey))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurityJson(route)
      val key = await(testAccount.generateAPICredentials(Some(testOrg.id)))
      key must_== returnedAPIKey
    }

    "delete api key" in new TestParameters {
      val testKey = GestaltAPIKey(
        apiKey = UUID.randomUUID().toString,
        apiSecret = Some("reallylongpassword"),
        accountId = testAccount.id,
        orgId = None,
        disabled = false
      )
      val url = baseUrl + s"/apiKeys/${testKey.id}"
      val route = (DELETE, url, Action {request =>
        Ok(Json.toJson(DeleteResult(true)))
      })
      implicit val security = getSecurity(route)
      await(testKey.delete()) must beTrue
    }

    "generate tokens against orgId from password grants using oauth2 standard" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}/oauth/issue"
      val now = DateTime.now()
      val newToken = OpaqueToken(UUID.randomUUID(),ACCESS_TOKEN)
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if (data.get("grant_type") == Some(Seq("password")) &&
                              data.get("username")   == Some(Seq("user")) &&
                              data.get("password")   == Some(Seq("pass"))) => Ok(Json.toJson(testTokenAuthResponse))
          case _ => BadRequest("was expecting form data")
        }
      })
      implicit val security = getSecurity(route)
      val resp = await(GestaltOrg.grantPasswordToken(testOrg.id, "user", "pass"))
      resp must beSome(testTokenAuthResponse)
    }

    "generate tokens against FQON from password grants using oauth2 standard" in new TestParameters {
      val url = baseUrl + s"/${testOrg.fqon}/oauth/issue"
      val now = DateTime.now()
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if (data.get("grant_type") == Some(Seq("password")) &&
            data.get("username")   == Some(Seq("user")) &&
            data.get("password")   == Some(Seq("pass"))) => Ok(Json.toJson(testTokenAuthResponse))
          case _ => BadRequest("was expecting form data")
        }
      })
      implicit val security = getSecurity(route)
      val resp = await(GestaltOrg.grantPasswordToken(testOrg.fqon, "user", "pass"))
      resp must beSome(testTokenAuthResponse)
    }

    "handle tokens request failure from password grant" in new TestParameters {
      val url = baseUrl + s"/${testOrg.fqon}/oauth/issue"
      val route = (POST, url, Action { BadRequest(Json.obj("error" -> "invalid_grant")) })
      implicit val security = getSecurity(route)
      val resp = await(GestaltOrg.grantPasswordToken(testOrg.fqon, "user", "pass"))
      resp must beNone
    }

    "globally introspect valid token on server" in new TestParameters {
      val url = baseUrl + s"/oauth/inspect"
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if data.get("token") == Some(Seq(testToken.toString)) => Ok(Json.toJson(testValidTokenResponse))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurity(route)
      val resp: TokenIntrospectionResponse = await(GestaltOrg.validateToken(testToken))
      resp must beAnInstanceOf[ValidTokenResponse]
    }

    "introspect valid token on server against fqon" in new TestParameters {
      val url = baseUrl + s"/${testOrg.fqon}/oauth/inspect"
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if data.get("token") == Some(Seq(testToken.toString)) => Ok(Json.toJson(testValidTokenResponse))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurity(route)
      val resp: TokenIntrospectionResponse = await(GestaltOrg.validateToken(testOrg.fqon, testToken))
      resp must beAnInstanceOf[ValidTokenResponse]
    }

    "introspect invalid token on server against fqon" in new TestParameters {
      val url = baseUrl + s"/${testOrg.fqon}/oauth/inspect"
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if data.get("token") == Some(Seq(testToken.toString)) => Ok(Json.toJson(INVALID_TOKEN))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurity(route)
      val resp: TokenIntrospectionResponse = await(GestaltOrg.validateToken(testOrg.fqon, testToken))
      resp must_== INVALID_TOKEN
    }

    "introspect valid token on server against org ID" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}/oauth/inspect"
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if data.get("token") == Some(Seq(testToken.toString)) => Ok(Json.toJson(testValidTokenResponse))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurity(route)
      val resp: TokenIntrospectionResponse = await(GestaltOrg.validateToken(testOrg.id, testToken))
      resp must beAnInstanceOf[ValidTokenResponse]
    }

    "introspect invalid token on server against org ID" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}/oauth/inspect"
      val route = (POST, url, Action { request =>
        request.body.asFormUrlEncoded match {
          case Some(data) if data.get("token") == Some(Seq(testToken.toString)) => Ok(Json.toJson(INVALID_TOKEN))
          case _ => BadRequest(Json.obj("error" -> "test conditions failed"))
        }
      })
      implicit val security = getSecurity(route)
      val resp: TokenIntrospectionResponse = await(GestaltOrg.validateToken(testOrg.id, testToken))
      resp must_== INVALID_TOKEN
    }

    "get an org by ID" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}"
      val route = (GET, url, Action {
        Ok(Json.toJson(testOrg))
      })
      implicit val security = getSecurity(route)
      val org: Option[GestaltOrg] = await(GestaltOrg.getById(testOrg.id))
      org must beSome(testOrg)
    }

    "get an org by FQON" in new TestParameters {
      val url = baseUrl + s"/${testOrg.fqon}"
      val route = (GET, url, Action {
        Ok(Json.toJson(testOrg))
      })
      implicit val security = getSecurity(route)
      val org: Option[GestaltOrg] = await(GestaltOrg.getByFQON(testOrg.fqon))
      org must beSome(testOrg)
    }

    "delete an org by ID" in new TestParameters {
      implicit val security = getMockSecurity
      security.deleteDR(s"orgs/${testOrg.id}") returns Future.successful(DeleteResult(true))
      val deleted = await(GestaltOrg.deleteOrg(testOrg.id))
      deleted must beTrue
    }

    "handle missing org with None" in new TestParameters {
      val orgId = UUID.randomUUID()
      val url = baseUrl + s"/orgs/${orgId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("orgId","org not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val org = await(GestaltOrg.getById(orgId))
      org must beNone
    }

    "list apps" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"App1",None,testOrg.id,false)
      val app2 = GestaltApp(UUID.randomUUID,"App2",None,testOrg.id,false)
      val testResp = Json.toJson( Seq(app1,app2) )
      val url = baseUrl + s"/orgs/${testOrg.id}/apps"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val apps = await(testOrg.listApps)
      apps must haveSize(2)
      apps must contain(app1)
      apps must contain(app2)
    }

    "get an app by name" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"App1",None,testOrg.id,false)
      val app2 = GestaltApp(UUID.randomUUID,"App2",None,testOrg.id,false)
      val testResp = Json.toJson( Seq(app1,app2) )
      val url = baseUrl + s"/orgs/${testOrg.id}/apps"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      await(testOrg.getAppByName(app2.name)) must beSome(app2)
      await(testOrg.getAppByName(app1.name)) must beSome(app1)
    }

    "create app" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}/apps"
      val createRequest = GestaltAppCreate(testApp.name)
      val route = (POST, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            // check parsing ability: gestalt-security uses this
            val c = js.as[GestaltAppCreate]
            if (c == createRequest) Created(Json.toJson(testApp))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val newApp = await(testOrg.createApp(createRequest))
      newApp must_== testApp
    }

    "list directories" in new TestParameters {
      val dir1 = GestaltDirectory(id = UUID.randomUUID, name = "dir1", Some("desc 1"), testOrg.id)
      val dir2 = GestaltDirectory(id = UUID.randomUUID, name = "dir2", Some("desc 2"), testOrg.id)
      val testResp = Json.toJson( Seq(dir1,dir2) )
      val url = baseUrl + s"/orgs/${testOrg.id}/directories"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val apps = await(testOrg.listDirectories())
      apps must_== Seq(dir1,dir2)
    }

    "create a directory" in new TestParameters {
      val createRequest = GestaltDirectoryCreate(testDir.name, DIRECTORY_TYPE_INTERNAL, testDir.description, config = None)
      val url = baseUrl + s"/orgs/${testOrg.id}/directories"
      val route = (POST, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            val c = js.as[GestaltDirectoryCreate]
            if (c == createRequest) Created(Json.toJson(testDir))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val dir = await(testOrg.createDirectory(createRequest))
      dir must_== testDir
    }

    "create a new org" in new TestParameters {
      implicit val security = getMockSecurity
      val parent = UUID.randomUUID()
      val createRequest = GestaltOrgCreate(testOrg.name, true, None)
      security.post[GestaltOrg](s"orgs/${parent}/orgs", Json.toJson(createRequest)) returns Future.successful(testOrg)
      val newOrg = await(GestaltOrg.createSubOrg(parentOrgId = parent, createRequest))
      newOrg must_== testOrg
    }

    "create new org with default args will create a user group" in new TestParameters {
      val createRequest = GestaltOrgCreate("some-name")
      createRequest.createDefaultUserGroup must beTrue
    }

    "authenticate framework users against specified org FQON" in new TestParameters {
      implicit val security = getMockSecurity
      val goodCreds = GestaltBasicCredentials("jdoe", "monkey")
      val badCreds = GestaltBasicCredentials("jdoe", "wrongPassword")
      val securityGood = mock[GestaltSecurityClient]
      val securityBad  = mock[GestaltSecurityClient]
      security.withCreds(goodCreds) returns securityGood
      security.withCreds(badCreds)  returns securityBad
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      securityGood.postEmpty[GestaltAuthResponse](s"${testOrg.fqon}/auth") returns
        Future{authResponse}
      securityBad.postEmpty[GestaltAuthResponse](s"${testOrg.fqon}/auth") returns
        Future.failed(UnauthorizedAPIException("","",""))

      await( GestaltOrg.authorizeFrameworkUser(testOrg.fqon, goodCreds) ) must beSome(authResponse)

      await( GestaltOrg.authorizeFrameworkUser(testOrg.fqon, badCreds) ) must beNone
    }

    "authenticate framework users against specified org UUID" in new TestParameters {
      val goodCreds = GestaltBasicCredentials("jdoe", "monkey")
      val badCreds = GestaltBasicCredentials("jdoe", "wrongPassword")
      implicit val security = getMockSecurity
      val securityGood = mock[GestaltSecurityClient]
      val securityBad  = mock[GestaltSecurityClient]
      security.withCreds(goodCreds) returns securityGood
      security.withCreds(badCreds)  returns securityBad
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      securityGood.postEmpty[GestaltAuthResponse](s"orgs/${testOrg.id}/auth") returns
        Future{authResponse}
      securityBad.postEmpty[GestaltAuthResponse](s"orgs/${testOrg.id}/auth") returns
        Future.failed(UnauthorizedAPIException("","",""))

      await(GestaltOrg.authorizeFrameworkUser(testOrg.id, goodCreds) ) must beSome(authResponse)
      await(GestaltOrg.authorizeFrameworkUser(testOrg.id, badCreds) ) must beNone
    }

    "authenticate framework users using API credentials" in new TestParameters {
      implicit val security = getMockSecurity
      val goodCreds = GestaltBasicCredentials("jdoe", "monkey")
      val badCreds = GestaltBasicCredentials("jdoe", "wrongPassword")
      val securityGood = mock[GestaltSecurityClient]
      val securityBad  = mock[GestaltSecurityClient]
      security.withCreds(goodCreds) returns securityGood
      security.withCreds(badCreds)  returns securityBad
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      securityGood.postEmpty[GestaltAuthResponse](s"auth") returns
        Future{authResponse}
      securityBad.postEmpty[GestaltAuthResponse](s"auth") returns
        Future.failed(UnauthorizedAPIException("","",""))

      await(GestaltOrg.authorizeFrameworkUser(goodCreds)) must beSome(authResponse)
      await(GestaltOrg.authorizeFrameworkUser(badCreds)) must beNone
    }

    "add user with groups to an org" in new TestParameters {
      implicit val security = getMockSecurity
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "admins", description = None, directory = testDir, disabled = false, accounts = Seq())

      val create = GestaltAccountCreateWithRights(
        username = testAccount.username,
        description = None,
        firstName = testAccount.firstName,
        lastName = testAccount.lastName,
        email = testAccount.email,
        phoneNumber = testAccount.phoneNumber,
        groups = Some(Seq(testGroup.id)),
        rights = Some(Seq(GestaltGrantCreate(testGrant.name))),
        credential = GestaltPasswordCredential("joe's password")
      )

      security.post[GestaltAccount](s"orgs/${testOrg.id}/accounts", Json.toJson(create)) returns
        Future{testAccount}

      val newAccount = await(GestaltOrg.createAccount(testOrg.id, create))

      newAccount must_== testAccount
    }

    "add group to an org" in new TestParameters {
      implicit val security = getMockSecurity
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "newGroup", description = None, directory = testDir, disabled = false, accounts = Seq())
      val authResponse = GestaltAuthResponse(testAccount, groups = Seq(testGroup), rights = Seq(testGrant), testOrg.id)

      val create = GestaltGroupCreateWithRights(
        name = testGroup.name,
        description = None,
        rights = Some(Seq(GestaltGrantCreate(testGrant.grantName)))
      )

      security.post[GestaltGroup](
        uri = s"orgs/${testOrg.id}/groups",
        payload = Json.toJson(create)
      ) returns Future.successful(testGroup)

      val newGroup = await(GestaltOrg.createGroup(testOrg.id, create))

      newGroup must_== testGroup
    }

  }

  "GestaltGroup" should {

    "list group accounts" in new TestParameters {
      implicit val security = getMockSecurity

      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "test", description = None, directory = testDir, disabled = false, accounts = Seq())

      val accountList = Seq(
        GestaltAccount(UUID.randomUUID(), "", "", "", None, "", "", testDir),
        GestaltAccount(UUID.randomUUID(), "", "", "", None, "", "", testDir)
      )
      mockGet(
        uri = s"groups/${testGroup.id}/accounts",
        maybeCreds = None,
        ret = accountList
      )

      await(testGroup.listAccounts()) must containTheSameElementsAs(accountList)
    }

    "update group membership" in new TestParameters {
      implicit val security = getMockSecurity

      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "test", description = None, directory = testDir, disabled = false, accounts = Seq())
      val addId = UUID.randomUUID()
      val remId = UUID.randomUUID()

      val updatedAccountList = Seq(GestaltAccount(addId, "", "", "", None, "", "", testDir).getLink())
      mockPatch(
        uri = s"groups/${testGroup.id}/accounts",
        payload = Json.toJson(Seq(
          PatchOp("add", "", Json.toJson(addId)),
          PatchOp("remove", "", Json.toJson(remId))
        )),
        maybeCreds = None,
        ret = updatedAccountList
      )
      await(testGroup.updateMembership(add = Seq(addId), remove = Seq(remId))) must containTheSameElementsAs(updatedAccountList)
    }

    "delete groups" in new TestParameters {
      implicit val security = getMockSecurity

      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "test", description = None, directory = testDir, disabled = false, accounts = Seq())
      security.deleteDR(s"groups/${testGroup.id}") returns Future.successful(DeleteResult(true))
      await(GestaltGroup.deleteGroup(testGroup.id)) must beTrue
    }

  }

  "GestaltAccount" should {

    "know thyself" in new TestParameters {
      val route = (GET, baseUrl + "/accounts/self", Action{Ok(Json.toJson(testAccount))} )
      implicit val security = getSecurity(route)
      await(GestaltAccount.getSelf()) must_== testAccount
    }

    "delete an account by ID" in new TestParameters {
      implicit val security = getMockSecurity
      security.deleteDR(s"accounts/${testAccount.id}") returns Future.successful(DeleteResult(true))
      await(GestaltAccount.deleteAccount(testAccount.id)) must beTrue
    }

    "list account groups" in new TestParameters {
      implicit val security = getMockSecurity

      val groupList = Seq(
        GestaltGroup(id = UUID.randomUUID, name = "test", description = None, directory = testDir, disabled = false, accounts = Seq()),
        GestaltGroup(id = UUID.randomUUID, name = "test", description = None, directory = testDir, disabled = false, accounts = Seq())
      )

      mockGet(
        uri = s"accounts/${testAccount.id}/groups",
        maybeCreds = None,
        ret = groupList
      )

      await(testAccount.listGroupMemberships()) must containTheSameElementsAs(groupList)
    }

  }

  "GestaltApp" should {

    "return a sane href" in new TestParameters {
      testApp.href must_== s"/apps/${testApp.id}"
    }

    "delete an app by ID" in new TestParameters {
      implicit val security = getMockSecurity
      security.deleteDR(s"apps/${testApp.id}") returns Future.successful(DeleteResult(true))
      await(GestaltApp.deleteApp(testApp.id)) must beTrue
    }

    "list all accounts" in new TestParameters {
      val acc1 = GestaltAccount(id = UUID.randomUUID, "mary", "M", "B", None, "", "", testDir)
      val acc2 = GestaltAccount(id = UUID.randomUUID, "john", "J", "S", None, "", "", testDir)
      val url = baseUrl + s"/apps/${testApp.id}/accounts"
      val route = (GET, url, Action { request =>
        Ok(Json.toJson(Seq(acc1,acc2)))
      })
      implicit val security = getSecurity(route)
      val accs = await(testApp.listAccounts)
      accs must haveSize(2)
      accs must contain(acc1)
      accs must contain(acc2)
    }

    "allow account creation" in new TestParameters {
      val createRequest = GestaltAccountCreateWithRights(
        username = testAccount.username,
        description = None,
        firstName = testAccount.firstName,
        lastName = testAccount.lastName,
        email = testAccount.email,
        phoneNumber = testAccount.phoneNumber,
        credential = GestaltPasswordCredential("kathy"),
        rights = Some(Seq(
          GestaltGrantCreate("strength", Some("11")),
          GestaltGrantCreate("empee:28abj38dja")
        ))
      )
      val url = baseUrl + s"/apps/${testApp.id}/accounts"
      val route = (POST, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            // check parsing ability: gestalt-security uses this
            val c = js.as[GestaltAccountCreateWithRights]
            // can't compare the objects directly; GestaltPasswordCredential doesn't have a credentialType field
            // instead, re-serialize, check the JSON
            if (Json.toJson(c).equals(Json.toJson(createRequest))) Created(Json.toJson(testAccount))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val newAccount = await(testApp.createAccount(createRequest))
      newAccount must_== testAccount
    }

    "list right grants" in new TestParameters {
      val grant1 = GestaltRightGrant(id = UUID.randomUUID, "grant1", None,           appId=testApp.id)
      val grant2 = GestaltRightGrant(id = UUID.randomUUID, "grant2", Some("value2"), appId=testApp.id)
      val testResp = Json.toJson( Seq(grant1,grant2) )
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testAccount.username}/rights"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val grants = await(testApp.listAccountGrants(testAccount.username))
      grants must_== Seq(grant1,grant2)
    }

    "list right grants for 404 returns failure" in new TestParameters {
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("username","resource missing","I have no idea what you're asking for."))) })
      implicit val security = getSecurity(route)
      await(testApp.listAccountGrants(testUsername)) must throwA[ResourceNotFoundException]
    }

    "list right grants for 400 returns failure" in new TestParameters {
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights"
      val route = (GET, url, Action { BadRequest(Json.toJson(BadRequestException("username","you did something bad","You've probably done something bad."))) })
      implicit val security = getSecurity(route)
      await(testApp.listAccountGrants(testUsername)) must throwA[BadRequestException]
    }

    "get a right grant by name" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${testGrant.grantName}"
      val route = (GET, url, Action {Ok(Json.toJson(testGrant))})
      implicit val security = getSecurity(route)
      val grant = await(testApp.getGrant(testAccountId.toString, testGrant.grantName))
      grant must beSome(testGrant)
    }

    "get a right grant on missing grant returns None" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${testGrant.grantName}"
      val route = (GET, url, Action {NotFound(Json.toJson(ResourceNotFoundException(s".../rights/${testGrant.grantName}","","")))})
      implicit val security = getSecurity(route)
      val grant = await(testApp.getGrant(testAccountId.toString, testGrant.grantName))
      grant must beNone
    }

    "get a right grant on missing account throws exception" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${testGrant.grantName}"
      val route = (GET, url, Action {NotFound(Json.toJson(ResourceNotFoundException(s".../accounts/${testAccountId}","","")))})
      implicit val security = getSecurity(route)
      await(testApp.getGrant(testAccountId.toString, testGrant.grantName)) must throwA[ResourceNotFoundException]
    }

    "get a right grant on missing app throws exception" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${testGrant.grantName}"
      val route = (GET, url, Action {NotFound(Json.toJson(ResourceNotFoundException(s".../apps/${testApp.id}","","")))})
      implicit val security = getSecurity(route)
      await(testApp.getGrant(testAccountId.toString, testGrant.grantName)) must throwA[ResourceNotFoundException]
    }

    "add a right grant by username" in new TestParameters {
      val testUsername = "testusername"
      val testAccountId = UUID.randomUUID
      val createGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights/${createGrant.grantName}"
      val route = (PUT, url, Action { Created(Json.toJson(createGrant)) })
      implicit val security = getSecurity(route)
      val newGrant = await(testApp.addGrant(testUsername,createGrant))
      newGrant must_== createGrant
    }

    "update a right grant by username" in new TestParameters {
      val testUsername = "testusername"
      val testAccountId = UUID.randomUUID
      val updateGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights/${updateGrant.grantName}"
      val route = (PUT, url, Action { Ok(Json.toJson(updateGrant)) })
      implicit val security = getSecurity(route)
      val newGrant = await(testApp.updateGrant(testUsername,updateGrant))
      newGrant must_== updateGrant
    }

    "delete extant right grant by username" in new TestParameters {
      val testUsername = "testusername"
      val testAccountId = UUID.randomUUID
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights/${testGrantName}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(true))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testApp.deleteGrant(testUsername,testGrantName))
      wasDeleted must_== true
    }

    "delete non-existant right grant" in new TestParameters {
      val testUsername = "someUsersName"
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.id}/usernames/${testUsername}/rights/${testGrantName}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(false))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testApp.deleteGrant(testUsername,testGrantName))
      wasDeleted must_== false
    }

    "get an app by ID" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"Test App",None,testOrg.id,false)
      val url = baseUrl + s"/apps/${app1.id}"
      val route = (GET, url, Action { Ok(Json.toJson(app1)) })
      implicit val security = getSecurity(route)
      val app = await(GestaltApp.getById(app1.id))
      app must beSome(app1)
    }

    "handle missing app with None" in new TestParameters {
      val appid = UUID.randomUUID()
      val url = baseUrl + s"/apps/${appid.toString}"
      val route = (GET, url, Action {
        NotFound(Json.toJson(ResourceNotFoundException("appId","app not found","Application with specified ID does not exist. Make sure you are using an application ID and not the application name.")))
      })
      implicit val security = getSecurity(route)
      val app = await(GestaltApp.getById(appid))
      app must beNone
    }

    "authenticate a user" in new TestParameters {
      val grant = GestaltRightGrant(id = UUID.randomUUID, "launcher:full_access",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), orgId = UUID.randomUUID())
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST, url, Action(BodyParsers.parse.json) { request =>
        if (request.body.equals(creds.toJson)) Ok(Json.toJson(authResponse))
        else Forbidden(Json.toJson(ForbiddenAPIException("failed to auth","failed to auth")))
      })
      implicit val security = getSecurityJson(route)
      val testResponse = await(testApp.authorizeUser(creds))
      testResponse must beSome(authResponse)
    }

    "throw on unexpected user authentication" in new TestParameters {
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST, url, Action {
        Forbidden(Json.toJson(ForbiddenAPIException("account authentication failed","Authentication of application account failed due to invalid account credentials.")))
      })
      implicit val security = getSecurity(route)
      await(testApp.authorizeUser(creds)) must throwA[ForbiddenAPIException]
    }

    "handle failed user authentication with a None" in new TestParameters {
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST, url, Action {
        NotFound(Json.toJson(ResourceNotFoundException("","account authentication failed","Authentication of application account failed due to invalid account credentials.")))
      })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.authorizeUser(creds))
      testResponse must beNone
    }

    "get a list of account store mappings" in new TestParameters {
      val m1 = GestaltAccountStoreMapping(UUID.randomUUID,"mapping1",Some("desc1"),DIRECTORY,UUID.randomUUID,testApp.id,false,true)
      val m2 = GestaltAccountStoreMapping(UUID.randomUUID,"mapping2",Some("desc2"),DIRECTORY,UUID.randomUUID,testApp.id,true,false)
      val url = baseUrl + s"/apps/${testApp.id}/accountStores"
      val route = (GET, url, Action { Ok(Json.toJson(Seq(m1,m2))) })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.listAccountStores)
      testResponse must_== Seq(m1,m2)
    }

    "perform account grant creation" in new TestParameters {
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccount.id}/rights"
      val grant = GestaltRightGrant(
        id = UUID.randomUUID(),
        grantName = "testGrant",
        grantValue = None,
        appId = testApp.id
      )
      val create = GestaltGrantCreate(grant.grantName)
      val route = (POST, url, Action { Ok(Json.toJson(grant)) })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.addAccountGrant(testAccount.id, create))
      testResponse must_== grant
    }

    "perform group grant creation" in new TestParameters {
      val testGroup = GestaltGroup(
        name = "testGroup",
        description = None,
        id = UUID.randomUUID(),
        directory = testDir,
        disabled = false,
        accounts = Seq()
      )
      val url = baseUrl + s"/apps/${testApp.id}/groups/${testGroup.id}/rights"
      val grant = GestaltRightGrant(
        id = UUID.randomUUID(),
        grantName = "testGrant",
        grantValue = None,
        appId = testApp.id
      )
      val create = GestaltGrantCreate(grant.grantName)
      val route = (POST, url, Action { Ok(Json.toJson(grant)) })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.addGroupGrant(testGroup.id, create))
      testResponse must_== grant
    }

    "create an account store mapping" in new TestParameters {
      val createRequest = GestaltAccountStoreMappingCreate(
        name = testMapping.name,
        description = testMapping.description,
        storeType = testMapping.storeType,
        accountStoreId = testMapping.storeId,
        isDefaultAccountStore = testMapping.isDefaultAccountStore,
        isDefaultGroupStore = testMapping.isDefaultGroupStore
      )
      val url = baseUrl + s"/apps/${testApp.id}/accountStores"
      val route = (POST, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            // check parsing ability: gestalt-security uses this
            val c = js.as[GestaltAccountStoreMappingCreate]
            if (c == createRequest) Created(Json.toJson(testMapping))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val newMapping = await(testApp.mapAccountStore(createRequest))
      newMapping must_== testMapping
    }

    "create account store mapping failure returns failed try" in new TestParameters {
      val createRequest = GestaltAccountStoreMappingCreate(
        name = testMapping.name,
        description = testMapping.description,
        storeType = testMapping.storeType,
        accountStoreId = testMapping.storeId,
        isDefaultAccountStore = testMapping.isDefaultAccountStore,
        isDefaultGroupStore = testMapping.isDefaultGroupStore
      )
      val url = baseUrl + s"/apps/${testApp.id}/accountStores"
      val route = (POST, url, Action {
        BadRequest(Json.toJson(BadRequestException("accountStores","some message","some developer message")))
      })
      implicit val security = getSecurity(route)
      await(testApp.mapAccountStore(createRequest)) must throwA[BadRequestException]
    }


  }

  "GestaltDirectory" should {

    "return a sane href" in new TestParameters {
      testDir.href must_== s"/directories/${testDir.id}"
    }

    "create account" in new TestParameters {
      val createRequest = GestaltAccountCreate(
        username = testAccount.username,
        description = None,
        firstName = testAccount.firstName,
        lastName = testAccount.lastName,
        email = testAccount.email,
        phoneNumber = testAccount.phoneNumber,
        credential = GestaltPasswordCredential("kathy")
      )
      val url = baseUrl + s"/directories/${testDir.id}/accounts"
      val route = (POST, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            // check parsing ability: gestalt-security uses this
            val c = js.as[GestaltAccountCreate]
            // can't compare the objects directly; GestaltPasswordCredential doesn't have a credentialType field
            // instead, re-serialize, check the JSON
            if (Json.toJson(c).equals(Json.toJson(createRequest))) Created(Json.toJson(testAccount))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val newUser = await(testDir.createAccount(createRequest))
      newUser must_== testAccount
    }

    "list directory accounts" in new TestParameters {
      val acc1 = GestaltAccount(id = UUID.randomUUID, "user1", "", "", None, "", "", testDir)
      val acc2 = GestaltAccount(id = UUID.randomUUID, "user2", "", "", None, "", "", testDir)
      val testResp = Json.toJson(Seq(acc1, acc2))
      val url = baseUrl + s"/directories/${testDir.id}/accounts"
      val route = (GET, url, Action {
        Ok(testResp)
      })
      implicit val security = getSecurity(route)
      val apps = await(testDir.listAccounts())
      apps must_== Seq(acc1, acc2)
    }

    "list directory accounts with override credentials" in new TestParameters {
      implicit val security = getMockSecurity
      val acc1 = GestaltAccount(id = UUID.randomUUID, "user1", "", "", None, "", "", testDir)
      val acc2 = GestaltAccount(id = UUID.randomUUID, "user2", "", "", None, "", "", testDir)
      val testResp = Seq(acc1, acc2)
      mockGet(
        uri = s"directories/${testDir.id}/accounts",
        maybeCreds = Some(testOverrideCreds),
        ret = testResp
      )
      val apps = await(testDir.listAccounts())
      apps must_== Seq(acc1, acc2)
    }

    "create user failure returns failed try" in new TestParameters {
      val createRequest = GestaltAccountCreate(
        username = "",
        description = None,
        firstName = "",
        lastName = "",
        email = "",
        phoneNumber = "",
        credential = GestaltPasswordCredential("")
      )
      val url = baseUrl + s"/directories/${testDir.id}/accounts"
      val route = (POST, url, Action {
        BadRequest(Json.toJson(BadRequestException("username", "some message", "some developer message")))
      })
      implicit val security = getSecurity(route)
      await(testDir.createAccount(createRequest)) must throwA[BadRequestException]
    }

    "get a directory by ID" in new TestParameters {
      val url = baseUrl + s"/directories/${testDir.id}"
      val route = (GET, url, Action { Ok(Json.toJson(testDir)) })
      implicit val security = getSecurity(route)
      val dir = await(GestaltDirectory.getById(testDir.id))
      dir must beSome(testDir)
    }

    "handle missing directory with None" in new TestParameters {
      val dirId = UUID.randomUUID()
      val url = baseUrl + s"/directories/${dirId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("dirId","dir not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val dir = await(GestaltDirectory.getById(dirId))
      dir must beNone
    }

  }

  "GestaltAccountStoreMapping" should {

    "return a sane href" in new TestParameters {
      testMapping.href must_== s"/accountStores/${testMapping.id}"
    }

    "get an account store mapping by ID" in new TestParameters {
      val url = baseUrl + s"/accountStores/${testMapping.id}"
      val route = (GET, url, Action { Ok(Json.toJson(testMapping)) })
      implicit val security = getSecurity(route)
      val asm = await(GestaltAccountStoreMapping.getById(testMapping.id))
      asm must beSome(testMapping)
    }

    "handle missing mapping with None" in new TestParameters {
      val badId = UUID.randomUUID()
      val url = baseUrl + s"/accountStores/${badId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("accountStoreMapping","mapping not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val asm = await(GestaltAccountStoreMapping.getById(badId))
      asm must beNone
    }

    "delete extant mapping" in new TestParameters {
      val url = baseUrl + s"/accountStores/${testMapping.id}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(true))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testMapping.delete())
      wasDeleted must_== true
    }

    "delete non-existant mapping" in new TestParameters {
      val url = baseUrl + s"/accountStores/${testMapping.id}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(false))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testMapping.delete())
      wasDeleted must_== false
    }

  }

}
