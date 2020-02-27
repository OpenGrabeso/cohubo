package com.github.opengrabeso.cohabo.common.css

import io.udash.css._

import scala.language.postfixOps

object GlobalStyles extends CssBase {

  import dsl._

  val floatRight: CssStyle = style(
    float.right
  )

  val messagesWindow: CssStyle = style(
    height :=! "calc(100vh - 220px)",
    overflowY.auto
  )

  val msgDate: CssStyle = style(
    marginLeft(5 px),
    fontSize(0.7 em),
    color.gray
  )

  val msgContainer: CssStyle = style(
    unsafeChild(s".${msgDate.className}")(
      display.none
    ),

    &.hover(
      unsafeChild(s".${msgDate.className}")(
        display.initial
      )
    )
  )

  val stravaImg: CssStyle = style(
    maxHeight.apply(46 px)
  )

  val footerText: CssStyle = style(
    color.darkblue
  )
  val footerLink: CssStyle = style(
    color.inherit
  )

  val logoutButton: CssStyle = style(
    justifyContent.spaceBetween
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

  val headerFooterCommon = mixin(
    backgroundColor(c"#def"),
    overflow.auto,
    flexGrow(0),
    flexDirection.row
  )

  style(
    unsafeRoot("#header") (headerFooterCommon)
  )

  style(
    unsafeRoot("#footer") (headerFooterCommon)
  )

  style(
    // we want the tables to be quite compact
    unsafeRoot("table .form-group")(
      marginBottom.`0`
    )
  )

  style(
    unsafeRoot("#edit-table .custom-select")(
      paddingTop.`0`,
      paddingBottom.`0`,
      height.inherit
    )
  )

}
