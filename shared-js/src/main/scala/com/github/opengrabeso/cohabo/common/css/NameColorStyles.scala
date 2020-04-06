package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object NameColorStyles extends CommonStyle {
  import dsl._

  // from http://phrogz.net/css/distinct-colors.html
  private val colors = Seq(
    c"#ffa799", c"#ffeecc", c"#ffeb99", c"#b4ff99", c"#99ffdd", c"#ccfffc", c"#99cfff", c"#99a7ff", c"#ff99eb", c"#ffcce7"
  )

  private val allColors = mixin(
    unsafeChild("a") (color.black)
  )

  val colorStyles = style(
    unsafeRoot(".repo-color-0")(allColors, backgroundColor(colors(0))),
    unsafeRoot(".repo-color-1")(allColors, backgroundColor(colors(1))),
    unsafeRoot(".repo-color-2")(allColors, backgroundColor(colors(2))),
    unsafeRoot(".repo-color-3")(allColors, backgroundColor(colors(3))),
    unsafeRoot(".repo-color-4")(allColors, backgroundColor(colors(4))),
    unsafeRoot(".repo-color-5")(allColors, backgroundColor(colors(5))),
    unsafeRoot(".repo-color-6")(allColors, backgroundColor(colors(6))),
    unsafeRoot(".repo-color-7")(allColors, backgroundColor(colors(7))),
    unsafeRoot(".repo-color-8")(allColors, backgroundColor(colors(8))),
    unsafeRoot(".repo-color-9")(allColors, backgroundColor(colors(9))),
  )

}
