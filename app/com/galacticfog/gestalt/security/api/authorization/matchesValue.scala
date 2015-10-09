package com.galacticfog.gestalt.security.api.authorization

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount}

case class matchesValue(grantName: String, grantValue: String)(matches: (String,String) => Boolean) extends AuthorizationCheck {
  override def isAuthorized(account: GestaltAccount, rights: Seq[GestaltRightGrant]): Boolean = {
    rights.exists( g =>
      g.grantName.equals(grantName) && g.grantValue.isDefined && matches(g.grantValue.get, grantValue)
    )
  }
}
