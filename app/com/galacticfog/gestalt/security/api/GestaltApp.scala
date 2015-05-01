package com.galacticfog.gestalt.security.api

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.galacticfog.gestalt.security.api.json.JsonImports._

case class GestaltApp(appId: String, appName: String, org: GestaltOrg) {
  def authorizeUser(creds: GestaltAuthToken)(implicit client: GestaltSecurityClient): Future[Option[GestaltAuthResponse]] = {
    client.post(s"apps/${appId}/auth",creds.toJson) map {
      _.asOpt[GestaltAuthResponse]
    } recover {
      case forbidden: ForbiddenAPIException => None
    }
  }
}

case object GestaltApp {

  def getAppById(appId: String)(implicit client: GestaltSecurityClient): Future[Option[GestaltApp]] = {
    client.get(s"apps/${appId}") map {
      _.asOpt[GestaltApp]
    } recover {
      case notFound: ResourceNotFoundException => None
    }
  }
}




