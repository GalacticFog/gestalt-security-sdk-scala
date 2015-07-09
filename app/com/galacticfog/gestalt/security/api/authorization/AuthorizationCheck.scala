package com.galacticfog.gestalt.security.api.authorization

import com.galacticfog.gestalt.security.api.{GestaltRightGrant, GestaltAccount}

trait AuthorizationCheck {
  def isAuthorized(account: GestaltAccount, rights: Seq[GestaltRightGrant]): Boolean
}
