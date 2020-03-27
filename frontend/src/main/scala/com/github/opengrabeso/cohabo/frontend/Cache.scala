package com.github.opengrabeso.cohabo.frontend

import scala.collection.mutable

class Cache[Key, T](size: Int, get: Key => T) {

  val data = mutable.Map.empty[Key, T]

  val accessTime = mutable.Map.empty[Key, Long]

  implicit object keyOrdering extends Ordering[Key] {
    override def compare(x: Key, y: Key) = {
      java.lang.Long.compare(accessTime(x), accessTime(y))
    }
  }

  val lastAccess = mutable.SortedSet.empty[Key]

  def apply(key: Key): T = {
    val now = System.currentTimeMillis()
    data.get(key) match {
      case Some(existing) =>
        lastAccess -= key
        accessTime += key -> now
        lastAccess += key
        existing
      case None =>
        val item = get(key)
        data += key -> item
        accessTime += key -> now // must be added before lastAccess, because its ordering will use it
        lastAccess += key
        if (data.size > size) {
          val first = lastAccess.firstKey
          val oldest = accessTime.minBy(_._2)._1
          assert(first == oldest)
          data -= first
          lastAccess -= first
          accessTime -= first
        }
        item
    }
  }

  def remove(key: Key): this.type = {
    data -= key
    this
  }

}
