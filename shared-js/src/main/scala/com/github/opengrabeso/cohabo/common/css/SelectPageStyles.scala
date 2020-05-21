package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.concurrent.duration._
import scala.language.postfixOps


object SelectPageStyles extends CommonStyle {

  import dsl._

  val textCenter = style(
    textAlign.center
  )

  val infoIcon = style(
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

  val settingsContainer = style(
    display.flex,
    flexDirection.column,
    margin.auto,
    containerBorder,
    height(100 %%),
  )

  val container = style(
    display.grid,
    margin.auto,
    containerBorder,
    height(100 %%),
    backgroundColor(c"#f7fcff"),
    gridTemplateColumns := "auto minmax(0, 1fr)",
    gridTemplateRows := "auto minmax(0, 1fr)",
    gridTemplateAreas(
      "nav filters",
      "nav table-container"
    ),
  )

  val labelButtons = style(
    width(25 vw),
    minWidth(200 px),
    display.flex,
    flexDirection.row,
    flexWrap.wrap
  )

  val labelButton = style(
    flexGrow(0)
  )

  val labelInline = style(
    margin(-1 px, 4 px),
    borderRadius(4 px),
    //border(solid, 1 px), // looks better, but makes rows higher
    padding(0 px, 4 px),
    display.inlineBlock,
    fontSize.small,
    //float.right // look nice, but causes some lines to be wrapped
  )

  val taskListProgress = style(
    backgroundColor(c"#c8c8c8"),
    fontSize.small,
    fontStyle.italic,
  )

  val userIcon = style(
    height(0.9 em),
    verticalAlign.baseline
  )

  val gridAreaNavigation = style(
    gridArea := "nav",
    display.flex,
    flexDirection.column
  )

  val gridAreaFilters = style(
    gridArea := "filters",
    display.flex,
    flexDirection.column
  )

  val filterExpression = style(
    flexDirection.row,
    flexGrow(1)
  )
  val filterExpressionInput = style(
    width(100 %%)
  )

  val gridAreaTableContainer = style(
    gridArea := "table-container",
    display.grid,
    gridTemplateRows := "1fr 0fr 10fr",
    gridTemplateAreas("table", "table-buttons", "article"),
  )

  val gridAreaArticle = style(
    gridArea := "article",
    display.flex,
    overflow.auto,
    flexDirection.column,
    maxWidth(60 em),
  )

  val gridAreaTableButtons = style(
    gridArea := "table-buttons",
    display.flex,
    flexDirection.row
  )

  val spin = keyframes(
    0.0 -> keyframe (),
    100.0 -> keyframe (transform := "rotate(360deg)")
  )

  val selectTableContainer = style(
    backgroundColor.white,
    gridArea := "table",
    overflow.auto,
    resize.vertical,
    maxHeight(70 vh),
    minHeight(10 vh),
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
    unsafeChild(".preview-fold") (
      color.lightgray,
    ),
    unsafeChild(".loading-fold") (
      display.inlineBlock,
      color.darkblue,
      animationName(spin),
      animationDuration(2 seconds),
      animationTimingFunction.linear,
      animationIterationCount.infinite
    ),
    unsafeChild(".no-fold") (
      color.lightgray,
    )

  )

  val tr = style(
    &.attrContains("class", "selected") (
      backgroundColor(c"#ADD8E6") // lightblue
    ),
    &.attrContains("class", "unread") (
      fontWeight.bold
    ),
    &.attrContains("class", "closed") (
      textDecoration := "line-through"
    ),
    &.attrContains("class", "unread-children") (
      // TODO: stronger highlight?
      fontWeight.bold,
      color(c"#808080")
    ),
    &.hover.attrContains("class", "selected") (
      backgroundColor(c"#A0D0E0")
    ),
    unsafeChild(".search-highlight") (
      backgroundColor(c"#fec"),
      fontWeight.bold
    )

  )

  val hightlightIssue = style(
    backgroundColor(c"#ffcc00"),

    &.hover (
      backgroundColor(c"#eebb00").important
    ),

    &.attrContains("class", "selected") (
      backgroundColor(c"#dd9900")
    ),
    &.hover.attrContains("class", "selected") (
      backgroundColor(c"#cc8800").important
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

  val cellRepo = mixin(
    lineHeight(1.1 rem),
    paddingBottom(2 px).important,
    paddingTop(2 px).important,
  )

  val td = style(cell)
  val th = style(cell)

  val tdRepo = style(cellRepo)


  val titleStyle = style(
    marginRight(8 px)
  )

  val uploading = style(
    backgroundColor.lightblue
  )
  val error = style(
    backgroundColor.red
  )

  val limitWidth = style(
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

  val articleMarkdown = style(
    // inspired by GitHub css
    unsafeRoot(".article-content") (
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
      ),
      unsafeChild(".lh-condensed") (
        lineHeight(1.25).important
      ),
      unsafeChild(".bg-gray-light") (
        backgroundColor(c"#fafbfc").important
      ),
      unsafeChild(".js-file-line-container") (
        unsafeChild(".tab-size[data-tab-size=\"8\"]") (
          tabSize := "8"
        )
      ),
      unsafeChild(".js-line-number") (
      ),
      unsafeChild(".blob-num") (
        width(1 %%),
        minWidth(50 px),
        textAlign.right,
        whiteSpace.nowrap,
        verticalAlign.top,
        fontFamily :=! "SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace",
        fontSize(12 px),
        lineHeight(20 px)
      ),
      unsafeChild(".blob-num:before") (
        content := "attr(data-line-number)"
      ),

      unsafeChild(".blob-code-inner") (
        fontFamily :=! "SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace",
        fontSize(12 px),
        color(c"#24292e"),
        wordWrap.normal,
        whiteSpace.pre
      ),
      unsafeChild(".js-file-line") (
      ),
      unsafeChild(".pl-k") (
        color(c"#d73a49")
      ),
      unsafeChild(".pl-en") (
        color(c"#6f42c1")
      ),
      unsafeChild(".pl-pds, .pl-s, .pl-s .pl-pse .pl-s1, .pl-sr, .pl-sr .pl-cce, .pl-sr .pl-sra, .pl-sr .pl-sre") (
        color(c"#032f62")
      ),
      unsafeChild(".pl-3, .px-3") (
        paddingLeft(16 px).important
      ),

      unsafeChild(".search-highlight") (
        backgroundColor(c"#fc8"),
        fontWeight.bold
      )
    )
  )

  val articleContentTextArea = style(
    overflow.auto,
    height(100 %%),
    borderStyle.solid,
    borderWidth(1 px),
    borderColor(c"#8080c0"),
    backgroundColor.white,
    marginTop(4 px),
    marginBottom(6 px), // avoid overflowing into the footer
    //unsafeChild(".article-content") (height(100 %%))
  )

  val titleEdit = style(
    overflow.auto,
    width(100 %%),
    margin(0.2 em, `0`)
  )

  val editArea = style(
    display.grid,
    height(100 %%),
    gridTemplateRows := "1fr auto",
    gridTemplateAreas("text", "buttons")
  )

  val editTextArea = style(
    gridArea := "text",
    overflow.auto,
    width(100 %%),
    height(100 %%),
    boxSizing.borderBox,
    resize.none
  )
  val editButtons = style(
    gridArea := "buttons",
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
