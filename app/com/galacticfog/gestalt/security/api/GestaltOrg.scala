package com.galacticfog.gestalt.security.api

import scala.concurrent.Future
import com.galacticfog.gestalt.security.api.json.JsonImports._
import scala.concurrent.ExecutionContext.Implicits.global

case class GestaltOrg(orgId: String, orgName: String) {
  def getApp(appId: GestaltApp)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    GestaltOrg.getApps(this) map {
      _.find {_.appId == appId}
    }
  }
}

case object GestaltOrg {

  def getApps(org: GestaltOrg)(implicit client: GestaltSecurityClient): Future[Seq[GestaltApp]] = {
    client.get(s"orgs/${org.orgId}/apps") map {
      _.as[Seq[GestaltApp]]
    }
  }

  def getDefaultOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = {
    client.get("orgs/default") map {
      _.as[GestaltOrg]
    }
  }

  @deprecated("Use getDefaultOrg","0.1.1")
  def getCurrentOrg(implicit client: GestaltSecurityClient): Future[GestaltOrg] = getDefaultOrg
}
