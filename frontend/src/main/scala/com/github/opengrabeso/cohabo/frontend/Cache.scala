package com.github.opengrabeso.cohabo.frontend

import scala.collection.mutable

class Cache[Key, T](size: Int, get: Key => T) {

  val data = mutable.Map.empty[Key, T]

  val lastAccess = mutable.Map.empty[Key, Long]

  def apply(key: Key): T = {
    val now = System.currentTimeMillis()
    val exists = data.get(key)
    if (exists.isDefined) {
      lastAccess += key -> now
      exists.get
    } else {
      val item = get(key)
      data += key -> item
      lastAccess += key -> now
      if (data.size > size) {
        // TODO: some smart data structure for this?
        val oldest = lastAccess.maxBy(a => now - a._2)._1
        data -= oldest
        lastAccess -= oldest
      }
      item
    }
  }

  def remove(key: Key): this.type = {
    data -= key
    this
  }

}
