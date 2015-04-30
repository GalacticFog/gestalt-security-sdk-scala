package com.galacticfog.gestalt.security.api

case class GestaltAuthResponse(account: GestaltAccount, rights: Seq[GestaltRightGrant])
