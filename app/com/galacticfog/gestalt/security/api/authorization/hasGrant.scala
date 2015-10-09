package com.galacticfog.gestalt.security.api.authorization

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount}

case class hasGrant(grantName: String) extends AuthorizationCheck {
  override def isAuthorized(account: GestaltAccount, rights: Seq[GestaltRightGrant]): Boolean = {
    rights.exists(_.grantName == grantName)
  }
}
