package com.github.opengrabeso.cohabo

class HighlightTest extends org.scalatest.funsuite.AnyFunSuite {
  test("Basic highlighting") {
    assert(Highlight("Abc xyz def", Seq("xyz")).contains(Highlight.decorate("xyz")))
  }

  test("No highlighting in HTML tags") {
    assert(!Highlight("<a href='xyz'>abc</a>", Seq("xyz")).contains(Highlight.decorate("xyz")))
    assert(Highlight("<a href='xyz'>abc</a>", Seq("abc")).contains(Highlight.decorate("abc")))
  }
}
