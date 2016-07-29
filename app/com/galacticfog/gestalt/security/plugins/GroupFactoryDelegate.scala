package com.galacticfog.gestalt.security.plugins

import java.util.UUID

import com.galacticfog.gestalt.io.util.PatchOp
import com.galacticfog.gestalt.security.api.{GestaltAccount, GestaltGroup}

import scala.util.Try


case class GroupMembership(accountId: UUID, groupId: UUID)

trait GroupFactoryDelegate {

  def find(groupId: UUID): Option[GestaltGroup]
  def delete(groupId: UUID): Boolean
  def create(name: String, description: Option[String], dirId: UUID, maybeParentOrg: Option[UUID]): Try[GestaltGroup]
  def lookupAppGroups(appId: UUID, nameQuery: String): Seq[GestaltGroup]
  def listByDirectoryId(dirId: UUID): Seq[GestaltGroup]
  def listGroupAccounts(groupId: UUID): Seq[GestaltAccount]
  def listAccountGroups(accountId: UUID): Seq[GestaltGroup]
  def findGroupMemberships(accountId: UUID, groupId: UUID): Option[GroupMembership]
  def removeAccountFromGroup(groupId: UUID, accountId: UUID): Unit
  def updateGroupMembership(groupId: UUID, payload: Seq[PatchOp]): Try[Seq[GestaltAccount]]
  def addAccountToGroup(groupId: UUID, accountId: UUID): Try[Seq[GestaltGroup]]
  def getAppGroupMapping(appId: UUID, groupId: UUID): Option[GestaltGroup]
  def queryShadowedAppGroups(appId: UUID, nameQuery: Option[String]): Seq[GestaltGroup]
  def findInDirectoryByName(dirId: UUID, groupName: String): Option[GestaltGroup]
  def queryShadowedDirectoryGroups(id: Option[UUID], groupName: Option[String]): List[GestaltGroup]

}
