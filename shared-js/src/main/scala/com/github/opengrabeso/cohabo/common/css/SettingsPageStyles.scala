package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object SettingsPageStyles extends CssBase{
  import dsl._

  val container: CssStyle = style(
    SelectPageStyles.containerBorder
  )
  val flexContainer: CssStyle = style(
    display.flex,
    flexDirection.row,
    flexWrap.wrap,
    margin.auto,
    flexGrow(1),
    media.maxWidth(900 px)(
      flexDirection.column
    ),
    media.maxAspectRatio(1 :/: 1)(
      flexDirection.column
    ),

  )
  val flexItem = style(
    flexGrow(1)
  )
}
