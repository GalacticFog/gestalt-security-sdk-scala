package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltAccountUpdate, GestaltGroup, GestaltPasswordCredential}

import scala.util.Try


trait DirectoryPlugin {

	def createAccount(username: String,
                   description: Option[String],
                   firstName: String,
                   lastName: String,
                   email: Option[String],
                   phoneNumber: Option[String],
                   cred: GestaltPasswordCredential): Try[GestaltAccount]

 def createGroup(name: String, description: Option[String]): Try[GestaltGroup]

 def updateAccount(account: GestaltAccount, update: GestaltAccountUpdate): Try[GestaltAccount]

 def authenticateAccount(account: GestaltAccount, plaintext: String): Boolean

 /** Directory-specific (i.e., deep) query of accounts, supporting wildcard matches on username, phone number or email address.
   *
   * Wildcard character '*' matches any number of characters; multiple wildcards may be present at any location in the query string.
   *
   * @param group  optional group search (no wildcard matching)
   * @param username username query parameter (e.g., "*smith")
   * @param phone phone number query parameter (e.g., "+1505*")
   * @param email email address query parameter (e.g., "*smith@company.com")
   * @return List of matching accounts (matching the query strings and belonging to the specified group)
   */
 def lookupAccounts(group: Option[GestaltGroup] = None,
                    username: Option[String] = None,
                    phone: Option[String] = None,
                    email: Option[String] = None): Seq[GestaltAccount]

 /**
   * Directory-specific (i.e., deep) query of groups, supporting wildcard matches on group name.
   *
   * Wildcard character '*' matches any number of character; multiple wildcards may be present at any location in the query string.
   *
   * @param groupName group name query parameter (e.g., "*-admins")
   * @return List of matching groups
   */
 def lookupGroups(groupName: String): Seq[GestaltGroup]

 def disableAccount(accountId: UUID, disabled: Boolean = true): Unit

 def deleteGroup(uuid: UUID): Boolean

 def getGroupById(groupId: UUID): Option[GestaltGroup]

 def listGroupAccounts(groupId: UUID): Seq[GestaltAccount]

 def id: UUID
 def name: String
 def description: Option[String]
 def orgId: UUID

}
