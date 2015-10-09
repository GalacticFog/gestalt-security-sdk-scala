package com.galacticfog.gestalt.security.api.authorization

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount}

case class hasValue(grantName: String, grantValue: String) extends AuthorizationCheck {
  override def isAuthorized(account: GestaltAccount, rights: Seq[GestaltRightGrant]): Boolean = {
    rights.exists( g =>
      g.grantName == grantName && g.grantValue.isDefined && g.grantValue.get.equals(grantValue)
    )
  }
}
