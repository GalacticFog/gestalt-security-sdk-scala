package com.galacticfog.gestalt.security.api

import java.util.UUID

@deprecated("this will be removedin favor of TokenIntrospectionResponse", "2.1.0")
case class GestaltAuthResponse(account: GestaltAccount,
                               groups: Seq[GestaltGroup],
                               rights: Seq[GestaltRightGrant],
                               orgId: UUID)
