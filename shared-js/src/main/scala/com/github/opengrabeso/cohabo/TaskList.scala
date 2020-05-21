package com.github.opengrabeso.cohabo

object TaskList {

  case class Progress(done: Int, total: Int) {
    def percent = done * 100 / total
  }

  private def findAllTasks(body: String): Seq[String] = {
    val task = "\\[([ xX])\\]".r
    val matches = task.findAllMatchIn(body)
    matches.map(_.group(1)).toSeq
  }

  def progress(body: String): Progress = {
    val all = findAllTasks(body)
    Progress(all.size - all.count(_ == " "), all.size)
  }


}
