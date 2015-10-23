package com.galacticfog.gestalt.security.api

case class GestaltAuthResponse(account: GestaltAccount, groups: Seq[GestaltGroup], rights: Seq[GestaltRightGrant])
