package com.galacticfog.gestalt.security.api

import scala.concurrent.Future
import com.galacticfog.gestalt.security.api.json.JsonImports._
import scala.concurrent.ExecutionContext.Implicits.global

case class GestaltOrg(orgId: String, orgName: String) {
}

case object GestaltOrg {
  def getApps(org: GestaltOrg)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get(s"orgs/${org.orgId}/apps") map {
      _.as[Seq[GestaltApp]]
    }
  }

  def getCurrentOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get("orgs/current") map {
      _.as[GestaltOrg]
    }
  }
}
