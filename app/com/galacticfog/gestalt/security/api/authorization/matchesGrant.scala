package com.galacticfog.gestalt.security.api.authorization

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount}

case class matchesGrant(testGrantName: String) extends AuthorizationCheck {
  import matchesGrant._

  val testSplit = splitAndValidate(testGrantName)

  def checkAuthorization(rights: Seq[GestaltRightGrant]): Boolean = {
    rights.exists(r => splitWildcardMatch(testSplit, splitAndValidate(r.grantName)))
  }

  override def isAuthorized(account: GestaltAccount, rights: Seq[GestaltRightGrant]): Boolean = {
    checkAuthorization(rights)
  }
}

case object matchesGrant {
  def splitAndValidate(name: String): List[String] = {
    if (name.trim.isEmpty) throw new RuntimeException("grant name must be non-empty")
    val split = name.trim.split(":")
    val firstSuper: Int = split.indexOf("**")
    if (0 <= firstSuper && firstSuper != split.size-1) throw new RuntimeException("invalid matcher; super-wildcard must be in the right-most field")
    split.toList
  }

  def splitWildcardMatch(a: List[String], b: List[String]): Boolean = {
    (a,b) match {
      case ( "**" :: aTail, _ ) => true
      case ( _, "**" :: bTail ) => true
      case ( Nil, Nil ) => true
      case ( aHead :: aTail, bHead :: bTail ) if (aHead == "*" || bHead == "*" || aHead == bHead) => splitWildcardMatch(aTail,bTail)
      case _ => false
    }
  }
}
