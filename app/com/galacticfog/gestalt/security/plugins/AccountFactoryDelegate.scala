package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.io.util.PatchOp
import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltAccountCreate, GestaltAccountUpdate, GestaltBasicCredsToken}

import scala.util.Try


trait AccountFactoryDelegate {

	def createAccount(dirId: UUID,
                   username: String,
                   description: Option[String] = None,
                   email: Option[String] = None,
                   phoneNumber: Option[String] = None,
                   firstName: String,
                   lastName: String,
                   hashMethod: String,
                   salt: String,
                   secret: String,
                   disabled: Boolean): Try[GestaltAccount]

	def disableAccount(accountId: UUID, disabled: Boolean): Unit

	def find(accountId: UUID): Option[GestaltAccount]

	def delete(accountId: UUID): Try[GestaltAccount]

	def findEnabled(accountId: UUID): Option[GestaltAccount]

   def listByDirectoryId(dirId: UUID): List[GestaltAccount]

   def updateAccount(account: GestaltAccount, patches: Seq[PatchOp]): Try[GestaltAccount]

   def updateAccountSDK(account: GestaltAccount, update: GestaltAccountUpdate): Try[GestaltAccount]

	def lookupByAppId(appId: UUID, nameQuery: Option[String], emailQuery: Option[String], phoneQuery: Option[String]): Seq[GestaltAccount]

	def authenticate(appId: UUID, creds: GestaltBasicCredsToken): Option[GestaltAccount]

	def getAppAccount(appId: UUID, accountId: UUID): Option[GestaltAccount]

	def listEnabledAppUsers(appId: UUID): List[GestaltAccount]

	def queryShadowedDirectoryAccounts(dirId: Option[UUID], nameQuery: Option[String], phoneQuery: Option[String], emailQuery: Option[String]): List[GestaltAccount]

	def findInDirectoryByName(dirId: UUID, username: String): Option[GestaltAccount]

}
