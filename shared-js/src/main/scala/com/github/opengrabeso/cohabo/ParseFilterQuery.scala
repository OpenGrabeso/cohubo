package com.github.opengrabeso.cohabo

import scala.util.parsing.combinator._

import QueryAST._

class ParseFilterQuery extends RegexParsers {

  override def skipWhitespace = false

  private val stateOpenQuery = "open" ^^ (_ => true)
  private val stateClosedQuery = "closed" ^^ ( _ => false)

  private def state: Parser[StateQuery] = ("is:" ~> (stateOpenQuery | stateClosedQuery)) ^^ StateQuery

  private def labelName = "[^ ]+".r
  private def label: Parser[LabelQuery] = ("label:" ~> labelName) ^^ LabelQuery

  def singleQuery: Parser[Query] = state | label
  def emptyQuery = success(Seq.empty[Query])
  def multipleQuery = (singleQuery ~ rep(whiteSpace ~> singleQuery)) ^^ {
    case head ~ tail =>
      tail.foldLeft(Seq(head))((s, q) => q +: s)
  }
  def query: Parser[Seq[Query]] = multipleQuery | emptyQuery
}


object ParseFilterQuery extends ParseFilterQuery {

  def apply(str: String): ParseResult[Seq[Query]] = {

    parseAll(query, str)

  }
}
