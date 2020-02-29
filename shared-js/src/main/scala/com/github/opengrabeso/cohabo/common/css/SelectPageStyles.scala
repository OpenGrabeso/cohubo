package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object SelectPageStyles extends CssBase {

  import dsl._

  val textCenter: CssStyle = style(
    textAlign.center
  )

  val infoIcon: CssStyle = style(
    fontSize(1 rem)
  )

  val containerBorder = mixin(
    margin(10 px),
    padding(5 px),
    borderColor.lightgray,
    borderRadius(10 px),
    borderStyle.solid,
    borderWidth(1 px)
  )

  val container: CssStyle = style(
    margin.auto,
    containerBorder,
  )

  val selectTableContainer: CssStyle = style(
    maxHeight(50 vh),
    overflow.auto
  )

  val tr: CssStyle = style(
    &.attrContains("class", "selected") (
      backgroundColor(c"#ADD8E6") // lightblue
    ),
    &.hover.attrContains("class", "selected") (
      backgroundColor(c"#A0D0E0")
    )
  )

  val td: CssStyle = style(
    lineHeight(1.0 rem),
    paddingBottom.`0`.important,
    paddingTop.`0`.important
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

  val articleContentTextArea: CssStyle = style(
    height(30 vh),
    minHeight(20 vh),
    maxHeight(50 vh)
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

  val hidden = style(
    display.none
  )

}
