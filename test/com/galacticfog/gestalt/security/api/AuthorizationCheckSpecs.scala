package com.galacticfog.gestalt.security.api

import java.util.UUID

import com.galacticfog.gestalt.security.api.authorization.{matchesGrant, matchesValue, hasValue, hasGrant}
import org.junit.runner._
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class AuthorizationCheckSpecs extends Specification with Mockito with Tables {

  def testApp = GestaltApp(id = UUID.randomUUID, name = "testApp", orgId = UUID.randomUUID, isServiceApp = false)
  def testDir = GestaltDirectory(id = UUID.randomUUID, name = "testDir", description = "test directory", orgId = testApp.orgId)
  def testAccount = GestaltAccount(id = UUID.randomUUID, "john", "John", "Doe", "jdoe@gmail.com", "850-867-5309", directory = testDir)

  def genRight(rightName: String, rightValue: Option[String]) = GestaltRightGrant(UUID.randomUUID,rightName,rightValue,appId=testApp.id)
  def genRight(rightName: String): GestaltRightGrant = genRight(rightName,None)

  "hasGrant" should {

    "match when solely present with no value" in {
      val grantName = "testGrant"
      val oneRight = Seq(genRight(grantName, None))
      hasGrant(grantName).isAuthorized(testAccount, oneRight) must beTrue
    }

    "match when solely present with Some value" in {
      val grantName = "testGrant"
      val oneRight = Seq(genRight(grantName, Some("value")))
      hasGrant(grantName).isAuthorized(testAccount, oneRight) must beTrue
    }

    "match when present amongst others" in {
      val grantName = "testGrant"
      val manyRights = Seq(
        genRight(grantName, None),
        genRight("anotherGrant",None),
        genRight("thirdGrant",None)
      )
      hasGrant(grantName).isAuthorized(testAccount, manyRights) must beTrue
    }

    "not match when none present" in {
      val noRights = Seq()
      hasGrant("testGrant").isAuthorized(testAccount, noRights) must beFalse
    }

    "not match when not present" in {
      val someRights = Seq(
        genRight("rightOne", None),
        genRight("rightTwo", None)
      )
      hasGrant("rightWrong").isAuthorized(testAccount, someRights) must beFalse
    }

  }

  "hasValue" should {

    "match when has named value" in {
      val right = Seq(genRight("foo",Some("bar")))
      hasValue("foo","bar").isAuthorized(testAccount, right) must beTrue
    }

    "not match when named value is None" in {
      val right = Seq(genRight("foo",None))
      hasValue("foo","bar").isAuthorized(testAccount, right) must beFalse
    }

  }

  "matchesValues" should {
    "match when values matches w.r.t. matcher" in {
      val n = "foo"
      val right = Seq(genRight(n,Some("BAR")))
      matchesValue(n,"bar"){_ equalsIgnoreCase _}.isAuthorized(testAccount, right) must beTrue
    }

    "not match when values don't match w.r.t matcher" in {
      val n = "foo"
      val v = "bar"
      val right = Seq(genRight(n,Some(v)))
      matchesValue(n,v){(_,_) => false}.isAuthorized(testAccount, right) must beFalse
    }

    "not match when name doesn't match" in {
      val right = Seq(genRight("foo1",Some("bar")))
      matchesValue("foo2","bar"){(_,_) => true}.isAuthorized(testAccount, right) must beFalse
    }

    "not match when value doesn't exist" in {
      val right = Seq(genRight("foo",None))
      matchesValue("foo","bar"){(_,_) => true}.isAuthorized(testAccount, right) must beFalse
    }
  }


  "matchesGrant" should {

    def testGrant(grantName: String, test: String) = matchesGrant(test).isAuthorized(testAccount, Seq(genRight(grantName)))

    "match when exact" in {
      "grant"         | "test"        |>
        "abc"         ! "abc"         |
        "abc:def"     ! "abc:def"     |
        "abc:def:ghi" ! "abc:def:ghi" | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "not match when exactly not" in {
      "grant"         | "test"        |>
        "abc"         ! "abC"         |
        "abc:def"     ! "ab:def"      |
        "abc:def"     ! "abc:de"      | { (grant,test) =>
        testGrant(grant,test) must beFalse
      }
    }

    "match wildcards on any single in grant or test" in {
      "grant"         | "test"        |>
        "*"           ! "abc"         |
        "abc"         ! "*"           |
        "*:def"       ! "abc:def"     |
        "abc:def"     ! "*:def"       |
        "abc:*:ghi"   ! "abc:def:ghi" |
        "abc:def:ghi" ! "abc:*:ghi"   |
        "*:def:*"     ! "abc:def:ghi" |
        "abc:def:ghi" ! "*:def:*"     |
        "*:def:*"     ! "abc:*:ghi"   | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "fail on unmatched sizes in the absence of a super-wildcard" in {
      "grant"         | "test"        |>
        "*"           ! "abc:def"     |
        "abc:def"     ! "*"           |
        "abc:def"     ! "abc:def:ghi" |
        "abc:def:ghi" ! "abc:def"     | { (grant,test) =>
        testGrant(grant,test) must beFalse
      }
    }

    "pass on rightmost super-wildcard" in {
      "grant"         | "test"        |>
        "**"          ! "a:b:c"       |
        "a:**"        ! "a:b:c"       |
        "a:b:**"      ! "a:b:c"       |
        "a:b:c:**"    ! "a:b:c"       |
        "a:b:c"       ! "**"          |
        "a:b:c"       ! "a:**"        |
        "a:b:c"       ! "a:b:**"      |
        "a:b:c"       ! "a:b:c:**"    | { (grant,test) =>
        testGrant(grant,test) must beTrue
      }
    }

    "throw exception when super-wildcard isn't rightmost" in {
      "grant"         | "test"    |>
        "**:b"        ! "a:b"     |
        "a:b"         ! "**:b"    |
        "a:**:c"      ! "a:b"     |
        "a:b"         ! "a:**:c"  | { (grant,test) =>
        testGrant(grant,test) must throwA[RuntimeException]("invalid matcher; super-wildcard must be in the right-most field")
      }
    }

    "throw exception when grant name is empty" in {
      "grant"  | "test"    |>
        ""     ! "a:b"     |
        "a:b"  ! ""        | { (grant,test) =>
        testGrant(grant,test) must throwA[RuntimeException]("grant name must be non-empty")
      }
    }

  }

}
