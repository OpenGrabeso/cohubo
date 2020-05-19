package com.github.opengrabeso.cohabo

import com.github.opengrabeso.cohabo.QueryAST._

class ParseFilterQueryTest extends org.scalatest.funsuite.AnyFunSuite {
  def testQuery(s: String, result: Seq[Query]) = {
    ParseFilterQuery(s) match {
      case ParseFilterQuery.Success(r, next) =>
        assert(next.atEnd)
        assert(r.toSet === result.toSet)
      case x: ParseFilterQuery.NoSuccess =>
        fail(x.msg)
    }
  }

  test("Parse empty query") {
    testQuery("", Seq.empty)
  }

  test("Parse simple state queries") {
    testQuery("is:open", Seq(StateQuery(true)))
    testQuery("is:closed", Seq(StateQuery(false)))
  }

  test("Reject malformed state queries") {
    assert(!ParseFilterQuery("is:opened").successful)
    assert(!ParseFilterQuery("is:close").successful)
    assert(!ParseFilterQuery("is:").successful)
  }

  test("Parse simple label queries") {
    testQuery("label:a", Seq(LabelQuery("a")))
  }

  test("Parse quoted label queries") {
    testQuery("label:\"a\"", Seq(LabelQuery("a")))
    testQuery("label:\"a b c\"", Seq(LabelQuery("a b c")))
  }

  test("Reject malformed label queries") {
    assert(!ParseFilterQuery("label:").successful)
  }

  test("Parse combined query") {
    assert(ParseFilterQuery("is:open label:bug label:wontfix").successful)
  }

  test("Fail on a query without spaces") {
    assert(!ParseFilterQuery("is:openlabel:bug").successful)
  }

  test("Fail on malformed queries") {
    assert(!ParseFilterQuery("is: label:bug").successful)
  }

  test("Parse a search query") {
    testQuery("something", Seq(SearchWordQuery("something")))
  }

  test("Parse a complex search query") {
    testQuery("is:open something else", Seq(StateQuery(true), SearchWordQuery("something"), SearchWordQuery("else")))
  }

  test("Label query produces proper text output") {
    assert(LabelQuery("a").toString == "label:a")
    assert(LabelQuery("a b").toString == "label:\"a b\"")
  }
}
