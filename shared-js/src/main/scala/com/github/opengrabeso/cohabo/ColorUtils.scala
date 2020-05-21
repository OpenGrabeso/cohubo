package com.github.opengrabeso.cohabo

object ColorUtils {
  type Color = (Double, Double, Double)

  implicit class TupleOps[X](c: (X, X, X)) {
    def map[R](f: X => R): (R, R, R) = (f(c._1),f(c._2),f(c._3))
    def map[Y, R](d: (Y, Y, Y))(f: (X, Y) => R): (R, R, R) = (f(c._1, d._1),f(c._2, d._2),f(c._3, d._3))
  }

  implicit class ColorOps(val c: Color) extends AnyVal {
    def * (f: Double): Color = c.map(_ * f)
    def * (x: Color): Color = c.map(x)(_ * _)
    def sum: Double = c._1 + c._2 + c._3
    def dot(x: Color): Double = (c * x).sum
    def toHex: String = {
      val strColor = c.map { x =>
        if (x < 0) "00"
        else if (x > 255) "FF"
        else f"${x.round.toInt}%02X"
      }
      "#" + strColor._1 + strColor._2 + strColor._3
    }
    def brightness: Double = c dot (0.299, 0.587, 0.114)
  }

  object Color {
    private def parseRGB(r: String, g: String, b: String) = {
      (Integer.parseInt(r, 16), Integer.parseInt(g, 16), Integer.parseInt(b, 16))
    }

    def parseHex(x: String): Color = {
      val RRGGBB = """([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})""".r
      val RGB = """([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])""".r
      x match {
        case RRGGBB(r, g, b) =>
          parseRGB(r, g, b).map(_.toDouble)
        case RGB(r, g, b) =>
          parseRGB(r, g, b).map(_ * 0x11).map(_.toDouble)
        case _ =>
          (0, 0, 0)
      }
    }
  }
}
