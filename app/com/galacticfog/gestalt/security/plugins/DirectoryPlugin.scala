package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.security.api.GestaltPasswordCredential

import scala.util.Try


trait DirectoryPlugin {

	def createAccount(username: String,
                   description: Option[String],
                   firstName: String,
                   lastName: String,
                   email: Option[String],
                   phoneNumber: Option[String],
                   cred: GestaltPasswordCredential): Try[UserAccountAdapter]

 def createGroup(name: String, description: Option[String]): Try[UserGroupAdapter]

 def updateAccount(account: UserAccountAdapter): Try[UserAccountAdapter]

 def authenticateAccount(account: UserAccountAdapter, plaintext: String): Boolean

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
 def lookupAccounts(group: Option[UserGroupAdapter] = None,
                    username: Option[String] = None,
                    phone: Option[String] = None,
                    email: Option[String] = None): Seq[UserAccountAdapter]

 /**
   * Directory-specific (i.e., deep) query of groups, supporting wildcard matches on group name.
   *
   * Wildcard character '*' matches any number of character; multiple wildcards may be present at any location in the query string.
   *
   * @param groupName group name query parameter (e.g., "*-admins")
   * @return List of matching groups
   */
 def lookupGroups(groupName: String): Seq[UserGroupAdapter]

 def disableAccount(accountId: UUID, disabled: Boolean = true): Unit

 def deleteGroup(uuid: UUID): Boolean

 def getGroupById(groupId: UUID): Option[UserGroupAdapter]

 def listGroupAccounts(groupId: UUID): Seq[UserAccountAdapter]

 def id: UUID
 def name: String
 def description: Option[String]
 def orgId: UUID

}
