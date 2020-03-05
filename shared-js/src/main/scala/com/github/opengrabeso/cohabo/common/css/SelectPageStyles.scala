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

  val passFlex = mixin(
    flexGrow(1),
    display.flex,
    flexDirection.column
  )

  val useFlex1 = style(
    passFlex
  )
  val useFlex0 = style(
    flexGrow(0)
  )

  val container: CssStyle = style(
    passFlex,
    margin.auto,
    containerBorder,
  )

  val selectTableContainer: CssStyle = style(
    passFlex,
    maxHeight(40 vh),
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

  val titleStyle = style(
    marginRight(8 px)
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

  val articleClose = style(
    unsafeRoot("table .rotate.down")(
      transform := "rotate(90deg)"
    )
  )

  val articleMarkdown = style(
    // inspired by GitHub css
    unsafeRoot(".article-content") (
      unsafeChild("pre") (
        padding(16 px),
        overflow.auto,
        fontSize(85 %%),
        lineHeight(1.45),
        backgroundColor(c"#f6f8fa"),
        borderRadius(3 px)
      ),
      unsafeChild("blockquote") (
        padding(`0`, 1 em),
        color(c"#6a737d"),
        borderLeft(.25 em, solid, c"#dfe2e5")
      )

    )
  )

  val articleContentTextArea: CssStyle = style(
    overflow.auto,
    minHeight(20 vh),
    maxHeight(30 vh),
    borderStyle.solid,
    borderWidth(1 px),
    borderColor(c"#8080c0")
  )

  val selectedArticle = style(
    unsafeChild(".title") (
      margin(0 px)
    ),
    unsafeChild(".link") (
      margin(8 px)
    ),
    unsafeChild(".createdBy") (
      color(c"#808080"),
      fontWeight.bold,
      margin(0 px)
    )
  )
}
