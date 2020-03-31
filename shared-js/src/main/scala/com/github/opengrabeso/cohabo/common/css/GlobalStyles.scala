package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object GlobalStyles extends CommonStyle {

  import dsl._

  val headerFooterCommon = mixin(
    backgroundColor(c"#def"),
    overflow.auto,
    display.flex,
    flexGrow(0),
    flexDirection.row
  )


  val rootContainer = style(
    display.grid,
    height(100 vh),
    gridTemplateRows := "auto minmax(0, 1fr) auto",
    gridTemplateAreas("header", "main", "footer"),

    unsafeChild("#header") (
      headerFooterCommon,
      gridArea := "header"
    ),

    unsafeChild("#footer") (
      headerFooterCommon,
      gridArea := "footer"
    ),

    unsafeChild("> div") (
      gridArea := "main"
    )
  )

  val floatRight: CssStyle = style(
    float.right
  )

  val footerText: CssStyle = style(
    color.darkblue
  )
  val footerLink: CssStyle = style(
    color.inherit
  )

  style(
    unsafeRoot("body")(
      display.flex,
      flexDirection.column,
      padding.`0`,
      margin.`0`,
      height(100 %%)
    ),
    unsafeRoot("html")(
      padding.`0`,
      margin.`0`,
      height(100 %%)
    ),
    unsafeRoot("#application")(
      display.flex,
      flexDirection.column,
      flexGrow(1)
    ),

    unsafeRoot(".container")(
      maxWidth(100 vw).important, // remove default Bootstrap width limitations
      display.flex,
      flexDirection.column,
      flexGrow(1)
    ),
    unsafeRoot(".container > div")(
      display.flex,
      flexDirection.column,
      flexGrow(1)
    )

  )


  val table = style(
    // we want the tables to be quite compact
    unsafeRoot("table .form-group")(
      marginBottom.`0`
    ),
    unsafeRoot("table .input-group-text")(
      paddingBottom.`0`,
      paddingTop.`0`
    )
  )

}
