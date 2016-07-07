package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.io.util.PatchOp

import scala.util.Try

trait GroupFactoryDelegate {

  def find(groupId: UUID): Option[UserGroupAdapter]
  def delete(groupId: UUID): Boolean
  def create(name: String, description: Option[String], dirId: UUID, maybeParentOrg: Option[UUID]): Try[UserGroupAdapter]
  def lookupAppGroups(appId: UUID, nameQuery: String): Seq[UserGroupAdapter]
  def listByDirectoryId(dirId: UUID): Seq[UserGroupAdapter]
  def listGroupAccounts(groupId: UUID): Seq[UserAccountAdapter]
  def removeAccountFromGroup(groupId: UUID, accountId: UUID): Unit
  def updateGroupMembership(groupId: UUID, payload: Seq[PatchOp]): Try[Seq[UserAccountAdapter]]
  def addAccountToGroup(groupId: UUID, accountId: UUID): Try[Seq[UserGroupAdapter]]
  def getAppGroupMapping(appId: UUID, groupId: UUID): Option[UserGroupAdapter]
  def queryShadowedAppGroups(appId: UUID, nameQuery: Option[String]): Seq[UserGroupAdapter]
  def findInDirectoryByName(dirId: UUID, groupName: String): Option[UserGroupAdapter]
  def queryShadowedDirectoryGroups(id: Option[UUID], groupName: Option[String])

}
