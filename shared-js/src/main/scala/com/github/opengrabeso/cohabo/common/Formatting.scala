package com.github.opengrabeso.cohabo.common

trait Formatting {
  def shortNameString(name: String, maxLen: Int = 30): String = {
    val ellipsis = "..."
    if (name.length < maxLen) name
    else {
      val allowed = name.take(maxLen-ellipsis.length)
      // prefer shortening about whole words
      val lastSeparator = allowed.lastIndexOf(' ')
      val used = if (lastSeparator >= allowed.length - 8) allowed.take(lastSeparator) else allowed
      used + ellipsis
    }
  }

  def displayDistance(dist: Double): String = "%.2f km".format(dist*0.001)

  def displaySeconds(duration: Int): String = {
    val hours = duration / 3600
    val secondsInHours = duration - hours * 3600
    val minutes = secondsInHours / 60
    val seconds = secondsInHours - minutes * 60
    if (hours > 0) {
      f"$hours:$minutes%02d:$seconds%02d"
    } else {
      f"$minutes:$seconds%02d"
    }
  }

  def normalizeSize(size: Double): String = {
    val units = Iterator("B", "KB", "MB", "GB", "TB")
    var selectedUnit = units.next()
    var sizeWithUnit = size
    while (sizeWithUnit >= 1024) {
      sizeWithUnit /= 1024
      selectedUnit = units.next()
    }
    "%.2f %s".format(sizeWithUnit, selectedUnit)
  }
}

object Formatting extends Formatting
