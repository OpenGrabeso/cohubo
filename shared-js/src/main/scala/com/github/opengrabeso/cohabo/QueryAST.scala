package com.github.opengrabeso.cohabo

object QueryAST {
  sealed trait Query
  case class LabelQuery(label: String) extends Query
  case class StateQuery(open: Boolean) extends Query
}
