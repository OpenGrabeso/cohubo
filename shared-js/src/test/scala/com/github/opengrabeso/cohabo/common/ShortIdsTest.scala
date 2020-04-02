package com.github.opengrabeso.cohabo.common

import ShortIds._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class ShortIdsTest extends org.scalatest.funsuite.AnyFunSuite with ScalaCheckDrivenPropertyChecks {
  override implicit val generatorDrivenConfig = PropertyCheckConfiguration(minSuccessful = 500)

  test("Initals construction") {
    val input = Seq("Name", "Surname")
    val init = Initials.from(input)
    assert(init.result.length == input.length) // some initials created, we do not care about the value
  }

  test("Construct initials for empty input") {
    val input = Seq.empty[String]
    val init = Initials.from(input)
    assert(init.result.length == input.length)
  }

  test("Construct initials for input containing empty string") {
    val input = Seq("")
    Initials.from(input)
    // we do not care about the result, we only want to verify no exception is thrown
  }

  test("Construct initials for all inputs") {
    forAll { input: Seq[String] =>
      val init = Initials.from(input)
      assert(init.result.length == input.count(_.nonEmpty)) // some initials created, we do not care about the value
    }
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

  test("Always generate something") {
    forAll(Gen.listOfN(50, Gen.listOfN(5, Gen.listOfN(10, Gen.alphaChar).map(_.mkString))) -> "input") { input =>
      val shorts = compute(input)
      assert(shorts.length == input.length)
    }
  }

}
