package com.github.opengrabeso.cohabo

object QueryAST {
  sealed trait Query
  sealed abstract class QueryWithName(prefix: String) extends Query {
    def name: String
    override def toString = prefix + ":" + (if (name.contains(' ')) "\"" + name + "\"" else name)
  }
  case class LabelQuery(name: String) extends QueryWithName("label")
  case class MilestoneQuery(name: String) extends QueryWithName("milestone")
  case class StateQuery(open: Boolean) extends Query {
    override def toString = if (open) "is:open" else "is:closed"
  }
  case class SearchWordQuery(word: String) extends Query {
    override def toString = word
  }
  case class AssignmentQuery(name: String) extends Query
}
