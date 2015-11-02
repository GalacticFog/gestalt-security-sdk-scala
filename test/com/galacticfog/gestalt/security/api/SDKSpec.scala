package com.galacticfog.gestalt.security.api

import java.util.{UUID}

import com.galacticfog.gestalt.security.api.errors._
import mockws.MockWS
import org.apache.commons.codec.binary.Base64
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.Scope
import play.api.libs.json.{JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.mvc._
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._
import com.galacticfog.gestalt.security.api.json.JsonImports._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

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

    val testOrg = GestaltOrg(UUID.randomUUID,"cdf.cu","cdf.cu",None,Seq())
    val testDir = GestaltDirectory(UUID.randomUUID,"Staff","CDF staff",testOrg.id)
    val testApp = GestaltApp(UUID.randomUUID,"inbox.cdf.cu",testOrg.id,false)
    val testAccount = GestaltAccount(
      id = UUID.randomUUID,
      username = "oldfart1",
      firstName = "John",
      lastName = "Perry",
      email = "jperry202@cdf.cu",
      phoneNumber = "850-867-5309",
      directoryId = testDir.id
    )
    val testMapping = GestaltAccountStoreMapping(UUID.randomUUID,"Staff Members","Staff members authorized for this app.",DIRECTORY,testDir.id,testApp.id,false,false)

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

  }

  "SDK client" should {

    "handle leading slash and no leading slash" in new TestParameters {
      val route = (GET, baseUrl + "/something", Action{Ok(Json.obj())} )
      val security = getSecurity(route)
      await(security.getJson("/something","","")).toString() must_== "{}"
      await(security.getJson("something","","")).toString() must_== "{}"
    }

    "handle failed API authentication with an exception" in new TestParameters {
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST,url, Action {
          Unauthorized(Json.toJson(UnauthorizedAPIException("API authentication failed","Authentication of API credentials failed.")))
        })
      implicit val security = getSecurity(route)
      await(testApp.authorizeUser(creds)) must beNone
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

    "neglect body and Content-Type on postTry(empty)" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (POST, url, Action {
        implicit request =>
          if (request.body.asText.exists(_.isEmpty) && request.body.asJson.isEmpty) Ok(Json.toJson(DeleteResult(true)))
          else BadRequest("")
      })
      val security = getSecurity(route)
      val deleted = await(security.postTry[DeleteResult]("something"))
      deleted must beSuccessfulTry(DeleteResult(true))
    }

    "returns UnknownAPIException on weird JSON error responses" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (GET, url, Action {
        BadRequest(Json.obj(
          "me" -> "not a SecurityRESTException"
        ))
      })
      val security = getSecurity(route)
      val error: Try[GestaltOrg] = await(security.getTry[GestaltOrg]("something"))
      error must beFailedTry.withThrowable[UnknownAPIException](".*could not parse to SecurityRESTException.*")
    }

    "returns UnknownAPIException on non-JSON error responses" in new TestParameters {
      val url = baseUrl + "/something"
      val route = (GET, url, Action {
        BadRequest("Not JSON")
      })
      val security = getSecurity(route)
      val error: Try[GestaltOrg] = await(security.getTry[GestaltOrg]("something"))
      error must beFailedTry.withThrowable[UnknownAPIException](".*could not parse to JSON.*")
    }

  }

  "GestaltOrg" should {

    "return a sane href" in new TestParameters {
      testOrg.href must_== s"/orgs/${testOrg.id}"
    }

    "support sync against org root" in new TestParameters {
      val chld = GestaltOrgWithChildren(UUID.randomUUID(), "child", "child", None, Seq())
      val root = GestaltOrgWithChildren(UUID.randomUUID(), "root", "root", None, Seq(chld))
      val jane = GestaltAccount(UUID.randomUUID(), username = "jdee", "Jane", "Dee", "jdee@org", "", UUID.randomUUID())
      val john = GestaltAccount(UUID.randomUUID(), username = "jdoe", "John", "Doe", "jdoe@chld.org", "", UUID.randomUUID())
      val rootUrl = baseUrl + "/sync"
      val route = (GET, rootUrl, Action {
        Ok(Json.toJson(GestaltOrgSync(
          accounts = Seq(
            GestaltOrgAccount.fromAccount(jane, Seq(chld.getLink, root.getLink)),
            GestaltOrgAccount.fromAccount(john, Seq(chld.getLink))
          ),
          orgTree = root
        )))
      })
      implicit val security = getSecurity(route)
      val rootSync = await(GestaltOrg.syncOrgTree(None, "username", "password"))
      rootSync.orgTree must_== root
      rootSync.accounts must_== Seq(
        GestaltOrgAccount.fromAccount(jane, Seq(chld.getLink, root.getLink)),
        GestaltOrgAccount.fromAccount(john, Seq(chld.getLink))
      )
    }

    "support sync against suborg" in new TestParameters {
      val chld = GestaltOrgWithChildren(UUID.randomUUID(), "child", "child", None, Seq())
      val jane = GestaltAccount(UUID.randomUUID(), username = "jdee", "Jane", "Dee", "jdee@org", "", UUID.randomUUID())
      val john = GestaltAccount(UUID.randomUUID(), username = "jdoe", "John", "Doe", "jdoe@chld.org", "", UUID.randomUUID())
      val chldUrl = baseUrl + s"/orgs/${chld.id}/sync"
      val route = (GET, chldUrl, Action {
        Ok(Json.toJson(GestaltOrgSync(
          accounts = Seq(
            GestaltOrgAccount.fromAccount(jane, Seq(chld.getLink)),
            GestaltOrgAccount.fromAccount(john, Seq(chld.getLink))
          ),
          orgTree = chld
        )))
      })
      implicit val security = getSecurity(route)
      val subSync = await(GestaltOrg.syncOrgTree(Some(chld.id), "username", "password"))
      subSync.orgTree must_== chld
      subSync.accounts must_== Seq(
        GestaltOrgAccount.fromAccount(jane, Seq(chld.getLink)),
        GestaltOrgAccount.fromAccount(john, Seq(chld.getLink))
      )
    }

    "return current org" in new TestParameters {
      val returnedOrg = GestaltOrg(id = UUID.randomUUID, "Test Org", "abcdefgh", None, Seq() )
      val url = baseUrl + "/orgs/current"
      val route = (GET, url, Action { Ok(Json.toJson(returnedOrg)) })
      implicit val security = getSecurity(route)
      val org = await(GestaltOrg.getCurrentOrg)
      org must_== returnedOrg
    }

    "get an org by ID" in new TestParameters {
      val url = baseUrl + s"/orgs/${testOrg.id}"
      val route = (GET, url, Action {
        Ok(Json.toJson(testOrg))
      })
      implicit val security = getSecurity(route)
      val org: Option[GestaltOrg] = await(GestaltOrg.getById(testOrg.id.toString))
      org must beSome(testOrg)
    }

    "delete an org by ID with auth override" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testUsername = "jdoe"
      val testPassword = "monkey"
      security.deleteTryWithAuth(s"orgs/${testOrg.id}", testUsername, testPassword) returns Future.successful(Success(DeleteResult(true)))
      val deleted: Try[Boolean] = await(GestaltOrg.deleteOrg(testOrg.id,testUsername,testPassword))
      deleted must beASuccessfulTry(true)
    }

    "delete an org by ID" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      security.deleteTry(s"orgs/${testOrg.id}") returns Future.successful(Success(DeleteResult(true)))
      val deleted: Try[Boolean] = await(GestaltOrg.deleteOrg(testOrg.id))
      deleted must beASuccessfulTry(true)
    }

    "handle missing org with None" in new TestParameters {
      val orgId = "missing"
      val url = baseUrl + s"/orgs/${orgId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("orgId","org not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val org = await(GestaltOrg.getById(orgId))
      org must beNone
    }

    "list apps" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"App1",testOrg.id,false)
      val app2 = GestaltApp(UUID.randomUUID,"App2",testOrg.id,false)
      val testResp = Json.toJson( Seq(app1,app2) )
      val url = baseUrl + s"/orgs/${testOrg.id}/apps"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val apps = await(testOrg.getApps)
      apps must haveSize(2)
      apps must contain(app1)
      apps must contain(app2)
    }

    "get an app by name" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"App1",testOrg.id,false)
      val app2 = GestaltApp(UUID.randomUUID,"App2",testOrg.id,false)
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
      newApp must beSuccessfulTry.withValue(testApp)
    }

    "list directories" in new TestParameters {
      val dir1 = GestaltDirectory(id = UUID.randomUUID, name = "dir1", "desc 1", testOrg.id)
      val dir2 = GestaltDirectory(id = UUID.randomUUID, name = "dir2", "desc 2", testOrg.id)
      val testResp = Json.toJson( Seq(dir1,dir2) )
      val url = baseUrl + s"/orgs/${testOrg.id}/directories"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val apps = await(testOrg.getDirectories())
      apps must_== Seq(dir1,dir2)
    }

    "create a directory" in new TestParameters {
      val createRequest = GestaltDirectoryCreate(testDir.name, testDir.description, orgId = testOrg.id)
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
      dir must beASuccessfulTry(testDir)
    }

    "create a new org" in new TestParameters {
      val parent = UUID.randomUUID()
      val createRequest = GestaltOrgCreate(testOrg.name)
      implicit val security = mock[GestaltSecurityClient]
      security.postTry[GestaltOrg](s"orgs/${parent}", Json.toJson(createRequest)) returns Future{Success{testOrg}}
      val newOrg = await(GestaltOrg.createSubOrg(parentOrgId = parent, createRequest.orgName))
      newOrg must beSuccessfulTry.withValue(testOrg)
    }

    "create a new org with auth override" in new TestParameters {
      val parent = UUID.randomUUID()
      val testUsername = "jdoe"
      val testPassword = "monkey"
      val createRequest = GestaltOrgCreate(testOrg.name)
      implicit val security = mock[GestaltSecurityClient]
      security.postTryWithAuth[GestaltOrg](s"orgs/${parent}", Json.toJson(createRequest), testUsername, testPassword) returns Future{Success{testOrg}}
      val newOrg = await(GestaltOrg.createSubOrg(parentOrgId = parent, createRequest.orgName, testUsername, testPassword))
      newOrg must beSuccessfulTry.withValue(testOrg)
    }

    "authenticate framework users against specified org FQON" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testUsername = "jdoe"
      val testPassword = "monkey"
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      security.postTryWithAuth[GestaltAuthResponse](s"${testOrg.fqon}/auth", testUsername, testPassword) returns
        Future{Success(authResponse)}
      security.postTryWithAuth[GestaltAuthResponse](s"${testOrg.fqon}/auth", testUsername, "wrongPassword") returns
        Future{Failure(UnauthorizedAPIException("",""))}

      val goodResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testOrg.fqon, testUsername, testPassword) )
      goodResponse must beSome(authResponse)

      val failResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testOrg.fqon, testUsername, "wrongPassword") )
      failResponse must beNone
    }

    "authenticate framework users against specified org UUID" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testUsername = "jdoe"
      val testPassword = "monkey"
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      security.postTryWithAuth[GestaltAuthResponse](s"orgs/${testOrg.id}/auth", testUsername, testPassword) returns
        Future{Success(authResponse)}
      security.postTryWithAuth[GestaltAuthResponse](s"orgs/${testOrg.id}/auth", testUsername, "wrongPassword") returns
        Future{Failure(UnauthorizedAPIException("",""))}

      val goodResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testOrg.id, testUsername, testPassword) )
      goodResponse must beSome(authResponse)

      val failResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testOrg.id, testUsername, "wrongPassword") )
      failResponse must beNone
    }

    "authenticate framework users using API credentials" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testKey = "jdoe"
      val testSecret = "monkey"
      val grant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val authResponse = GestaltAuthResponse(testAccount, Seq(), Seq(grant), UUID.randomUUID())
      security.postTryWithAuth[GestaltAuthResponse](s"auth", testKey, testSecret) returns
        Future{Success(authResponse)}
      security.postTryWithAuth[GestaltAuthResponse](s"auth", testKey, "wrongSecret") returns
        Future{Failure(UnauthorizedAPIException("",""))}

      val goodResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testKey, testSecret) )
      goodResponse must beSome(authResponse)

      val failResponse: Option[GestaltAuthResponse] = await(GestaltOrg.authorizeFrameworkUser(testKey, "wrongSecret") )
      failResponse must beNone
    }

    "add user with groups to an org with auth override" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testUsername = "jdoe"
      val testPassword = "monkey"
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "admins", directoryId = testAccount.directoryId, disabled = false)

      val create = GestaltAccountCreateWithRights(
        username = testAccount.username,
        firstName = testAccount.firstName,
        lastName = testAccount.lastName,
        email = testAccount.email,
        phoneNumber = testAccount.phoneNumber,
        groups = Some(Seq(testGroup.id)),
        rights = Some(Seq(GestaltGrantCreate(testGrant.name))),
        credential = GestaltPasswordCredential("joe's password")
      )

      security.postTryWithAuth[GestaltAccount](s"orgs/${testOrg.id}/accounts", Json.toJson(create), testUsername, testPassword) returns
        Future{Success(testAccount)}

      val newAccount: Try[GestaltAccount] = await(GestaltOrg.createAccount(testOrg.id, create, testUsername, testPassword))

      newAccount must beSuccessfulTry(testAccount)
    }

    "add user with groups to an org" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "admins", directoryId = testAccount.directoryId, disabled = false)

      val create = GestaltAccountCreateWithRights(
        username = testAccount.username,
        firstName = testAccount.firstName,
        lastName = testAccount.lastName,
        email = testAccount.email,
        phoneNumber = testAccount.phoneNumber,
        groups = Some(Seq(testGroup.id)),
        rights = Some(Seq(GestaltGrantCreate(testGrant.name))),
        credential = GestaltPasswordCredential("joe's password")
      )

      security.postTry[GestaltAccount](s"orgs/${testOrg.id}/accounts", Json.toJson(create)) returns
        Future{Success(testAccount)}

      val newAccount: Try[GestaltAccount] = await(GestaltOrg.createAccount(testOrg.id, create))

      newAccount must beSuccessfulTry(testAccount)
    }

    "add group to an org" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "newGroup", directoryId = testAccount.directoryId, disabled = false)
      val authResponse = GestaltAuthResponse(testAccount, groups = Seq(testGroup), rights = Seq(testGrant), testOrg.id)

      val create = GestaltGroupCreateWithRights(
        name = testGroup.name,
        rights = Some(Seq(testGrant))
      )

      security.postTry[GestaltGroup](s"orgs/${testOrg.id}/groups", Json.toJson(create)) returns
        Future{Success(testGroup)}

      val newGroup: Try[GestaltGroup] = await(GestaltOrg.createGroup(testOrg.id, create))

      newGroup must beSuccessfulTry(testGroup)
    }

    "add group to an org with auth override" in new TestParameters {
      implicit val security = mock[GestaltSecurityClient]
      val testGrant = GestaltRightGrant(id = UUID.randomUUID, "createSubOrg",None, appId = testApp.id)
      val testGroup = GestaltGroup(id = UUID.randomUUID, name = "newGroup", directoryId = testAccount.directoryId, disabled = false)
      val authResponse = GestaltAuthResponse(testAccount, groups = Seq(testGroup), rights = Seq(testGrant), testOrg.id)
      val testUsername = "jdoe"
      val testPassword = "monkey"

      val create = GestaltGroupCreateWithRights(
        name = testGroup.name,
        rights = Some(Seq(testGrant))
      )

      security.postTryWithAuth[GestaltGroup](s"orgs/${testOrg.id}/groups", Json.toJson(create), testUsername, testPassword) returns
        Future{Success(testGroup)}

      val newGroup: Try[GestaltGroup] = await(GestaltOrg.createGroup(testOrg.id, create, testUsername, testPassword))

      newGroup must beSuccessfulTry(testGroup)
    }

  }

  "GestaltApp" should {

    "return a sane href" in new TestParameters {
      testApp.href must_== s"/apps/${testApp.id}"
    }

    "list all accounts" in new TestParameters {
      val acc1 = GestaltAccount(id = UUID.randomUUID, "mary", "M", "B", "", "", testDir.id)
      val acc2 = GestaltAccount(id = UUID.randomUUID, "john", "J", "S", "", "", testDir.id)
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
      newAccount must beSuccessfulTry.withValue(testAccount)
    }

    "list right grants" in new TestParameters {
      val testAccountId = UUID.randomUUID()
      val grant1 = GestaltRightGrant(id = UUID.randomUUID, "grant1", None,           appId=testApp.id)
      val grant2 = GestaltRightGrant(id = UUID.randomUUID, "grant2", Some("value2"), appId=testApp.id)
      val testResp = Json.toJson( Seq(grant1,grant2) )
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights"
      val route = (GET, url, Action { Ok(testResp) })
      implicit val security = getSecurity(route)
      val grants = await(testApp.listGrants(testAccountId.toString))
      grants must beSuccessfulTry(Seq(grant1,grant2))
    }

    "list right grants for 404 returns failure" in new TestParameters {
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testUsername}/rights"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("username","resource missing","I have no idea what you're asking for."))) })
      implicit val security = getSecurity(route)
      val grants = await(testApp.listGrants(testUsername))
      grants must beFailedTry.withThrowable[ResourceNotFoundException]
    }

    "list right grants for 400 returns failure" in new TestParameters {
      val testUsername = "someUsersName"
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testUsername}/rights"
      val route = (GET, url, Action { BadRequest(Json.toJson(BadRequestException("username","you did something bad","You've probably done something bad."))) })
      implicit val security = getSecurity(route)
      val grants = await(testApp.listGrants(testUsername))
      grants must beFailedTry.withThrowable[BadRequestException]
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

    "add a right grant" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val createGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${createGrant.grantName}"
      val route = (PUT, url, Action { Created(Json.toJson(createGrant)) })
      implicit val security = getSecurity(route)
      val newGrant = await(testApp.addGrant(testAccountId.toString,createGrant))
      newGrant must beSuccessfulTry(createGrant)
    }

    "update a right grant" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val updateGrant = GestaltRightGrant(id = UUID.randomUUID, "testGrantName", Some("testGrantValue"), appId = testApp.id)
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${updateGrant.grantName}"
      val route = (PUT, url, Action { Ok(Json.toJson(updateGrant)) })
      implicit val security = getSecurity(route)
      val newGrant = await(testApp.updateGrant(testAccountId.toString,updateGrant))
      newGrant must beSuccessfulTry(updateGrant)
    }

    "delete extant right grant" in new TestParameters {
      val testAccountId = UUID.randomUUID
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testAccountId}/rights/${testGrantName}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(true))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testApp.deleteGrant(testAccountId.toString,testGrantName))
      wasDeleted must beSuccessfulTry(true)
    }

    "delete non-existant right grant" in new TestParameters {
      val testUsername = "someUsersName"
      val testGrantName = "someGrant"
      val url = baseUrl + s"/apps/${testApp.id}/accounts/${testUsername}/rights/${testGrantName}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(false))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testApp.deleteGrant(testUsername,testGrantName))
      wasDeleted must beSuccessfulTry(false)
    }

    "get an app by ID" in new TestParameters {
      val app1 = GestaltApp(UUID.randomUUID,"Test App",testOrg.id,false)
      val url = baseUrl + s"/apps/${app1.id}"
      val route = (GET, url, Action { Ok(Json.toJson(app1)) })
      implicit val security = getSecurity(route)
      val app = await(GestaltApp.getById(app1.id.toString))
      app must beSome(app1)
    }

    "handle missing app with None" in new TestParameters {
      val appid = "missing"
      val url = baseUrl + s"/apps/${appid}"
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

    "handle failed user authentication with a None" in new TestParameters {
      val creds = GestaltBasicCredsToken("jdoe","monkey")
      val url = baseUrl + s"/apps/${testApp.id}/auth"
      val route = (POST, url, Action {
        Forbidden(Json.toJson(ForbiddenAPIException("account authentication failed","Authentication of application account failed due to invalid account credentials.")))
      })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.authorizeUser(creds))
      testResponse must beNone
    }

    "get a list of account store mappings" in new TestParameters {
      val m1 = GestaltAccountStoreMapping(UUID.randomUUID,"mapping1","desc1",DIRECTORY,UUID.randomUUID,testApp.id,false,true)
      val m2 = GestaltAccountStoreMapping(UUID.randomUUID,"mapping2","desc2",DIRECTORY,UUID.randomUUID,testApp.id,true,false)
      val url = baseUrl + s"/apps/${testApp.id}/accountStores"
      val route = (GET, url, Action { Ok(Json.toJson(Seq(m1,m2))) })
      implicit val security = getSecurity(route)
      val testResponse = await(testApp.listAccountStores)
      testResponse must_== Seq(m1,m2)
    }

  }

  "GestaltDirectory" should {

    "return a sane href" in new TestParameters {
      testDir.href must_== s"/directories/${testDir.id}"
    }

    "create account" in new TestParameters {
      val createRequest = GestaltAccountCreate(
        username = testAccount.username,
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
      newUser must beSuccessfulTry.withValue(testAccount)
    }

    "list directory accounts" in new TestParameters {
      val acc1 = GestaltAccount(id = UUID.randomUUID, "user1", "", "", "", "", testDir.id)
      val acc2 = GestaltAccount(id = UUID.randomUUID, "user2", "", "", "", "", testDir.id)
      val testResp = Json.toJson(Seq(acc1, acc2))
      val url = baseUrl + s"/directories/${testDir.id}/accounts"
      val route = (GET, url, Action {
        Ok(testResp)
      })
      implicit val security = getSecurity(route)
      val apps = await(testDir.getAccounts())
      apps must_== Seq(acc1, acc2)
    }

    "create user failure returns failed try" in new TestParameters {
      val createRequest = GestaltAccountCreate(
        username = "",
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
      val failedUser = await(testDir.createAccount(createRequest))
      failedUser must beFailedTry.withThrowable[BadRequestException]
    }

    "get a directory by ID" in new TestParameters {
      val url = baseUrl + s"/directories/${testDir.id}"
      val route = (GET, url, Action { Ok(Json.toJson(testDir)) })
      implicit val security = getSecurity(route)
      val dir = await(GestaltDirectory.getById(testDir.id.toString))
      dir must beSome(testDir)
    }

    "handle missing directory with None" in new TestParameters {
      val dirId = "missing"
      val url = baseUrl + s"/directories/${dirId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("dirId","dir not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val dir = await(GestaltDirectory.getById(dirId))
      dir must beNone
    }

  }

  "GestaltAccountStoreMapping" should {

    "return a sane href" in new TestParameters {
      testMapping.href must_== s"/accountStoreMappings/${testMapping.id}"
    }

    "get an account store mapping by ID" in new TestParameters {
      val url = baseUrl + s"/accountStoreMappings/${testMapping.id}"
      val route = (GET, url, Action { Ok(Json.toJson(testMapping)) })
      implicit val security = getSecurity(route)
      val asm = await(GestaltAccountStoreMapping.getById(testMapping.id.toString))
      asm must beSome(testMapping)
    }

    "handle missing mapping with None" in new TestParameters {
      val badId = "badMappingId"
      val url = baseUrl + s"/accountStoreMappings/${badId}"
      val route = (GET, url, Action { NotFound(Json.toJson(ResourceNotFoundException("accountStoreMapping","mapping not found","blah blah blah"))) })
      implicit val security = getSecurity(route)
      val asm = await(GestaltAccountStoreMapping.getById(badId))
      asm must beNone
    }

    "delete extant mapping" in new TestParameters {
      val url = baseUrl + s"/accountStoreMappings/${testMapping.id}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(true))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testMapping.delete())
      wasDeleted must beSuccessfulTry(true)
    }

    "delete non-existant mapping" in new TestParameters {
      val url = baseUrl + s"/accountStoreMappings/${testMapping.id}"
      val route = (DELETE, url, Action { Ok(Json.toJson(DeleteResult(false))) })
      implicit val security = getSecurity(route)
      val wasDeleted = await(testMapping.delete())
      wasDeleted must beSuccessfulTry(false)
    }

    "create an account store mapping" in new TestParameters {
      val createRequest = GestaltAccountStoreMappingCreate(
        name = testMapping.name,
        description = testMapping.description,
        storeType = testMapping.storeType,
        accountStoreId = testMapping.storeId,
        appId = testMapping.appId,
        isDefaultAccountStore = testMapping.isDefaultAccountStore,
        isDefaultGroupStore = testMapping.isDefaultGroupStore
      )
      val url = baseUrl + "/accountStoreMappings"
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
      val newMapping = await(GestaltAccountStoreMapping.createMapping(createRequest))
      newMapping must beSuccessfulTry(testMapping)
    }

    "create account store mapping failure returns failed try" in new TestParameters {
      val createRequest = GestaltAccountStoreMappingCreate(
        name = testMapping.name,
        description = testMapping.description,
        storeType = testMapping.storeType,
        accountStoreId = testMapping.storeId,
        appId = testMapping.appId,
        isDefaultAccountStore = testMapping.isDefaultAccountStore,
        isDefaultGroupStore = testMapping.isDefaultGroupStore
      )
      val url = baseUrl + "/accountStoreMappings"
      val route = (POST, url, Action {
        BadRequest(Json.toJson(BadRequestException("accountStoreMappings","some message","some developer message")))
      })
      implicit val security = getSecurity(route)
      val failedMapping = await(GestaltAccountStoreMapping.createMapping(createRequest))
      failedMapping must beFailedTry.withThrowable[BadRequestException]
    }

    "update account store mapping" in new TestParameters {
      // we don't care about the values, we're just checking that the client makes the right call
      val updateRequest = GestaltAccountStoreMappingUpdate(
        id = testMapping.id,
        name = testMapping.name + "whatever",
        description = testMapping.description + "whatever",
        isDefaultAccountStore = !testMapping.isDefaultAccountStore,
        isDefaultGroupStore = !testMapping.isDefaultGroupStore
      )
      val url = baseUrl + s"/accountStoreMappings/${testMapping.id}"
      val route = (PUT, url, Action { request =>
        request.body.asJson match {
          case Some(js) =>
            // check parsing ability: gestalt-security uses this
            val u = js.as[GestaltAccountStoreMappingUpdate]
            if (u == updateRequest) Ok(Json.toJson(testMapping))
            else BadRequest("did not get the json body I was expecting")
          case None => BadRequest("was expecting json")
        }
      })
      implicit val security = getSecurity(route)
      val updated = await(testMapping.update(updateRequest))
      updated must beSuccessfulTry.withValue(testMapping)
    }

  }

}
