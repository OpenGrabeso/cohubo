package com.github.opengrabeso.cohabo

import scala.util.parsing.combinator._

import QueryAST._

class ParseFilterQuery extends RegexParsers {

  override def skipWhitespace = false

  private val stateOpenQuery = "open" ^^ (_ => true)
  private val stateClosedQuery = "closed" ^^ ( _ => false)

  private def state: Parser[StateQuery] = ("is:" ~> (stateOpenQuery | stateClosedQuery)) ^^ StateQuery

  private def name = "[^ ]+".r
  private def quotedName: Parser[String] = "\"" ~> "[^\"]+".r <~ "\""
  private def label: Parser[LabelQuery] = ("label:" ~> (quotedName | name)) ^^ LabelQuery
  private def assignee: Parser[AssigneeQuery] = ("assignee:" ~> (quotedName | name)) ^^ AssigneeQuery
  private def search: Parser[SearchWordQuery] = "[^ :]+".r ^^ SearchWordQuery

  def singleQuery: Parser[Query] = state | label | assignee | search
  def emptyQuery: Parser[Seq[Query]] = success(Seq.empty[Query])
  def multipleQuery: Parser[Seq[Query]] = (singleQuery ~ rep(whiteSpace ~> singleQuery)) ^^ {
    case head ~ tail =>
      tail.foldLeft(Seq(head))((s, q) => s :+ q)
  }
  def query: Parser[Seq[Query]] = multipleQuery | emptyQuery
}


object ParseFilterQuery extends ParseFilterQuery {

  def apply(str: String): ParseResult[Seq[Query]] = {

    parseAll(query, str)

  }
}
