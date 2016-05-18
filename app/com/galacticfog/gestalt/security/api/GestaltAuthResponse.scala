package com.galacticfog.gestalt.security.api

import java.util.UUID

case class GestaltAuthResponse(account: GestaltAccount,
                               groups: Seq[GestaltGroup],
                               rights: Seq[GestaltRightGrant],
                               orgId: UUID)
