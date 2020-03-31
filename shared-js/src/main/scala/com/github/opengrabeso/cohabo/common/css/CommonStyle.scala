package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

trait CommonStyle extends CssBase {

  import dsl._

  val passFlex = mixin(
    flexGrow(1),
    display.flex,
    flexDirection.column
  )

  val flexRow = style(
    display.flex,
    flexDirection.row
  )

  val passFlex1 = style(
    passFlex
  )
  val useFlex1 = style(
    flexGrow(1)
  )
  val useFlex0 = style(
    flexGrow(0)
  )
}
