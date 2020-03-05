package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object EditPageStyles extends CssBase {

  import dsl._

  val container: CssStyle = style(
    marginTop(10 px),

    flexGrow(1),
    display.flex,
    flexDirection.row,
    media.maxWidth(900 px)(
      flexDirection.column
    ),
    media.maxAspectRatio(1 :/: 1)(
      flexDirection.column
    ),
    flexWrap.wrap,

    padding(5 px),
    borderColor.lightgray,
    borderRadius(10 px),
    borderStyle.solid,
    borderWidth(1 px)
  )

  val tableContainer: CssStyle = style(
    overflow.auto
  )

}
