package com.github.opengrabeso.cohabo

object TaskList {

  case class Progress(done: Int, total: Int) {
    def percent = done * 100 / total
  }

  private def findAllTasks(body: String): Seq[String] = {
    val Task = "\\s*[-+*0-9.)]+\\s+\\[([ xX])\\].*".r
    body.linesIterator.collect {
      case Task(check) =>
        check
    }.toSeq
  }

  def progress(body: String): Progress = {
    val all = findAllTasks(body)
    Progress(all.size - all.count(_ == " "), all.size)
  }


}
