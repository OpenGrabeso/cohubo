package com.github.opengrabeso.cohabo
package frontend.dataModel

import java.time.ZonedDateTime
import common.Util._
import io.udash.HasModelPropertyCreator

case class UnreadInfo(
  updatedAt: ZonedDateTime,
  lastReadAt: ZonedDateTime
) {
  // when we have already read everything, the unread mark must be by user, return everything as unread
  def markedByUser: Boolean = lastReadAt >= updatedAt
  def isUnread(time: ZonedDateTime): Boolean = {
    markedByUser || time > lastReadAt
  }
}

object UnreadInfo extends HasModelPropertyCreator[UnreadInfo]
