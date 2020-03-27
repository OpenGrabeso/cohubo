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

  val flexRow = style(
    display.flex,
    flexDirection.row
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
    overflow.auto,
    border(2 px, solid, darkgray),
    // see https://stackoverflow.com/a/56998444/16673
    unsafeChild("thead tr th") (
      position.sticky.important,
      top(0 px).important,
      backgroundColor(c"#f0f0f0"),
      borderLeft(1 px, dotted, rgba(200, 209, 224, 0.6)),
      borderBottom(1 px, solid, c"#e8e8e8"),
      borderTop(2 px, solid, c"#f0f0f0")
    ),
    unsafeChild(".fold-control") (
      color.darkgray,
      fontWeight._900
    ),
    unsafeChild(".no-fold") (
      color.lightgray,
    )

  )

  val tr: CssStyle = style(
    &.attrContains("class", "selected") (
      backgroundColor(c"#ADD8E6") // lightblue
    ),
    &.attrContains("class", "unread") (
      // TODO: stronger highlight?
      fontWeight.bold
    ),
    &.attrContains("class", "unread-children") (
      // TODO: stronger highlight?
      fontWeight.bold,
      color(c"#808080")
    ),
    &.hover.attrContains("class", "selected") (
      backgroundColor(c"#A0D0E0")
    )
  )

  val cell = mixin(
    lineHeight(1.0 rem),
    paddingBottom.`0`.important,
    paddingTop.`0`.important,
    maxWidth(20 px), // allow free resizing
    unsafeChild("div") (
      // divs inside of table cells should never wrap, they should silently overflow
      whiteSpace.nowrap,
      overflow.hidden
    )
  )

  val td: CssStyle = style(cell)
  val th: CssStyle = style(cell)


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
      ),
      unsafeChild("blockquote") (
        padding(`0`, 1 em),
        color(c"#6a737d"),
        borderLeft(.25 em, solid, c"#dfe2e5")
      ),
      unsafeChild("pre") (
        backgroundColor(rgba(27,31,35,.05))
      ),
      unsafeChild("pre code, pre tt") (
        color(c"#000000"),
        padding.`0`,
        margin.`0`,
        overflow.auto,
        fontSize(85 %%),
        lineHeight(1.45),
        backgroundColor.initial,
        borderRadius(3 px),
        border.`0`
      ),
      unsafeChild("pre>code") (
        fontSize(100 %%),
        wordBreak.normal,
        whiteSpace.pre,
        backgroundColor.transparent
      ),
      unsafeChild("code") (
        color(c"#000000"),
        margin.`0`,
        padding(0.2 em, 0.4 em),
        backgroundColor(rgba(27,31,35,.05)),
        fontSize(85 %%),
        borderRadius(3 px)
      ),
      unsafeChild("table") (
        unsafeChild("td, th") (
          padding(6 px, 13 px),
          border(1 px, solid, c"#dfe2e5"),
        ),
        unsafeChild("tr:nth-child(2n)") (
          backgroundColor(c"#f6f8fa"),
        )
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

  val editTextArea = style(
    overflow.auto,
    width(100 %%),
    boxSizing.borderBox
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
