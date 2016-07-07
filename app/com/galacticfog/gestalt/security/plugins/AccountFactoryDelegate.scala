package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.io.util.PatchOp
import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltAccountUpdate, GestaltBasicCredsToken}

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
                   disabled: Boolean): Try[UserAccountAdapter]

	def disableAccount(accountId: UUID, disabled: Boolean): Unit

	def find(accountId: UUID): Option[UserAccountAdapter]

	def findEnabled(accountId: UUID): Option[UserAccountAdapter]

   def listByDirectoryId(dirId: UUID): List[UserAccountAdapter]

	def checkPassword(account: GestaltAccount, plaintext: String): Boolean

	def saveAccount(account: UserAccountAdapter): Try[UserAccountAdapter]

   def updateAccount(account: UserAccountAdapter, patches: Seq[PatchOp]): Try[UserAccountAdapter]

   def updateAccountSDK(account: UserAccountAdapter, update: GestaltAccountUpdate): Try[UserAccountAdapter]

	def lookupByAppId(appId: UUID, nameQuery: Option[String], emailQuery: Option[String], phoneQuery: Option[String]): Seq[UserAccountAdapter]

	def authenticate(appId: UUID, creds: GestaltBasicCredsToken): Option[UserAccountAdapter]

	def getAppAccount(appId: UUID, accountId: UUID): Option[UserAccountAdapter]

	def listEnabledAppUsers(appId: UUID): List[UserAccountAdapter]

	def queryShadowedDirectoryAccounts(dirId: Option[UUID], nameQuery: Option[String], phoneQuery: Option[String], emailQuery: Option[String]): List[UserAccountAdapter]

	def findInDirectoryByName(dirId: UUID, username: String): Option[UserAccountAdapter]

}
