package com.galacticfog.gestalt.security.plugins


case class DirectoryAdapter(id: Any, name: String, description: Option[String] = None, orgId: Any, config: Option[String] = None, directoryType: String)

case class UserAccountAdapter(id: Any, dirId: Any, username: String, email: Option[String] = None, phoneNumber: Option[String] = None, firstName: String,
                                lastName: String, hashMethod: String, salt: String, secret: String, disabled: Boolean, description: Option[String] = None)

case class UserGroupAdapter(id: Any, dirId: Any, name: String, disabled: Boolean, parentOrg: Option[Any] = None, description: Option[String] = None)

case class GroupMembershipAdapter(accountId: Any, groupId: Any)

