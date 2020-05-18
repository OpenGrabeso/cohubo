package com.github.opengrabeso.cohabo

object QueryAST {
  sealed trait Query
  case class LabelQuery(name: String) extends Query
  case class MilestoneQuery(name: String) extends Query
  case class StateQuery(open: Boolean) extends Query
  case class SearchWordQuery(word: String) extends Query
  case class AssignmentQuery(name: String) extends Query
}
