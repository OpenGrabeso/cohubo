package com.github.opengrabeso.cohabo

import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import ColorUtils._

class ColorUtilsTest extends org.scalatest.funsuite.AnyFunSuite with ScalaCheckDrivenPropertyChecks {
  test("Parse RRGGBB") {
    assert( Color.parseHex("000000") == (0, 0, 0))
    assert( Color.parseHex("ffffff") == (255, 255, 255))
    assert( Color.parseHex("112233") == (0x11, 0x22, 0x33))
  }

  test("Multiply color") {
    assert(Color.parseHex("080808") * 4 == (0x20, 0x20, 0x20))
  }

  test("Clamp color") {
    assert((Color.parseHex("080808") * 100).toHex == "#FFFFFF")
    assert((Color.parseHex("080808") * -1).toHex == "#000000")
  }

  test("Brightness ordering") {
    assert(Color.parseHex("000").brightness < Color.parseHex("111").brightness)
    assert(Color.parseHex("000").brightness < Color.parseHex("fff").brightness)
    assert(Color.parseHex("789").brightness < Color.parseHex("78a").brightness)
    assert(Color.parseHex("456").brightness < Color.parseHex("656").brightness)
  }
}
