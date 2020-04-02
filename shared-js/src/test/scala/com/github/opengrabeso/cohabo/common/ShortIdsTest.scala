package com.github.opengrabeso.cohabo.common

import ShortIds._

class ShortIdsTest extends org.scalatest.FunSuite {
  test("Initals construction") {
    val init = Initials.from(Seq("Name", "Surname"))
    assert(init.result.length == 2) // some initials created, we do not care about the value
  }

  test("Some initials should be constructed") {
    val input = Seq(Seq("Name", "Surname"))
    val list = compute(input)
    assert(list.length == 1)
  }

  test("Initials should be unique even for similiar or identical name") {
    val input = Seq(Seq("Name", "Surname"), Seq("Name", "Surname"), Seq("Name", "Surrname"))
    val list = compute(input)
    assert(list.length == input.length)
    assert(list.distinct.length == input.length)
  }
}
