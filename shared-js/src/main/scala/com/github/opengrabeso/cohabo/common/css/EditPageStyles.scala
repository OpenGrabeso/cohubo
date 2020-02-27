package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object EditPageStyles extends CssBase {

  import dsl._

  val textCenter: CssStyle = style(
    textAlign.center
  )

  val infoIcon: CssStyle = style(
    fontSize(1 rem)
  )

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
    maxHeight(600 px),
    media.maxWidth(900 px)(
      // map below, keep table height low
      maxHeight(400 px)
    ),
    media.maxAspectRatio(1 :/: 1)(
      maxHeight(400 px)
    ),
    overflow.auto
  )

  val uploading: CssStyle = style(
    backgroundColor.lightblue
  )
  val error: CssStyle = style(
    backgroundColor.red
  )

  val limitWidth: CssStyle = style(
    maxWidth(500 px)
  )

  val inputDesc: CssStyle = style (
    // ignored, overridden by default Bootstrap styles, need to use different method (Bootstrap theming?}
    backgroundColor.transparent,
    border.none
  )

  val inputName : CssStyle = style (
    // ignored, overridden by default Bootstrap styles, need to use different method (Bootstrap theming?}
    backgroundColor.transparent,
    border.none
  )


  private val minWide = 1000 px

  val wideMedia = style(
    media.not.all.minWidth(minWide)(
      display.none
    )
  )
  val narrowMedia = style(
    media.minWidth(minWide)(
      display.none
    )
  )

  val mapBase = mixin(
    flexGrow(1),
    paddingLeft(5 px),
    top(0 px),
    bottom(0 px),
    minWidth(200 px),
    minHeight(200 px)
  )
  val map: CssStyle = style(
    mapBase

  )
  val noMap: CssStyle = style(
    mapBase,
    backgroundColor(c"#efc")
  )

}
