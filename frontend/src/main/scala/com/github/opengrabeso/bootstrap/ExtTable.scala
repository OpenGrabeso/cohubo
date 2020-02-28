package com.github.opengrabeso.bootstrap

import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.UdashBootstrap
import io.udash.bootstrap.utils.{BootstrapStyles, UdashBootstrapComponent}
import io.udash.component.ComponentId
import io.udash.component.Components._
import io.udash.properties.seq
import io.udash.wrappers.jquery.{JQuery, jQ}
import org.scalajs.dom._
import scalatags.JsDom.all._

import scala.scalajs.js
import scala.scalajs.js.|

final class ExtTable[ItemType, ElemType <: ReadableProperty[ItemType]] private(
  items: seq.ReadableSeqProperty[ItemType, ElemType],
  responsive: ReadableProperty[Option[BootstrapStyles.ResponsiveBreakpoint]],
  dark: ReadableProperty[Boolean],
  striped: ReadableProperty[Boolean],
  bordered: ReadableProperty[Boolean],
  borderless: ReadableProperty[Boolean],
  hover: ReadableProperty[Boolean],
  small: ReadableProperty[Boolean],
  override val componentId: ComponentId
)(
  captionFactory: Option[Binding.NestedInterceptor => Modifier],
  headerFactory: Option[Binding.NestedInterceptor => Modifier],
  rowFactory: (ElemType, Binding.NestedInterceptor) => Element
) extends UdashBootstrapComponent {

  import io.udash.css.CssView._

  override val render: Element = {
    div(
      nestedInterceptor((BootstrapStyles.Table.responsive _).reactiveOptionApply(responsive)),
      table(
        id := componentId,
        //BootstrapStyles.Table.table,
        attr("data-toggle") := "table",
        attr("data-search") := "true",
        attr("data-click-to-select") := "true",
        attr("data-single-select") := "true",
        attr("data-resizable"):="true",
        //attr("data-columns") := "true",
        nestedInterceptor(BootstrapStyles.Table.dark.styleIf(dark)),
        nestedInterceptor(BootstrapStyles.Table.striped.styleIf(striped)),
        nestedInterceptor(BootstrapStyles.Table.bordered.styleIf(bordered)),
        nestedInterceptor(BootstrapStyles.Table.borderless.styleIf(borderless)),
        nestedInterceptor(BootstrapStyles.Table.hover.styleIf(hover)),
        nestedInterceptor(BootstrapStyles.Table.small.styleIf(small))
      )(
        captionFactory.map(content => caption(content(nestedInterceptor)).render),
        headerFactory.map(head => thead(head(nestedInterceptor)).render),
        tbody(
          nestedInterceptor(
            repeatWithNested(items) { case (item, nested) =>
              rowFactory(item, nested)
            }
          )
        )
      ),
      script {
        ExtTable.callback = { t =>
          t.asInstanceOf[js.Dynamic].bootstrapTable()
          val rows = t.find("tr")
          val rowSelectTd = rows.find("td:first-of-type")
          val checkboxes = rowSelectTd.find("input")
          rows
            //.on("select", { (el, ev) => println(s"  rows 'select' on $el ${ev.`type`}") })
            .on("click", { (el, ev) =>
              println("Row clicked")
              jQ(el).find("td").removeAttr("style") }
            )
            //.on("check", { (el, ev) => println(s"  rows 'check' on $el ${ev.`type`}") })
          //
          val selected = rows.filter("[selected]")
          val checked = checkboxes.filter("[checked]")
          println(s"selected ${selected.length}")
          checked.removeAttr("checked")

          // something sets them all to selected - but unselect here does not help much
          // that is why we set background-color explicitly
          rows.removeAttr("selected")
          rows.find("td").attr("style","background-color: white")
          // checkbox needs to be present for click-to-select to work, but it can be hidden
          rowSelectTd.find("label").hide()
          // calling click here does not work unfortunately
          //rows.first().trigger("click")

        }
        s"ExtTable.callback($$('#$componentId'))"
      }
    ).render
  }
}

@js.annotation.JSExportTopLevel("ExtTable")
object ExtTable {

  /**
    * Creates a table component.
    * More: <a href="http://getbootstrap.com/docs/4.1/content/tables/">Bootstrap Docs</a>.
    *
    * @param items          Elements which will be rendered as the table rows.
    * @param responsive     If defined, the table will be horizontally scrollable on selected screen size.
    * @param dark           Switch table to the dark theme.
    * @param striped        Turn on zebra-striping.
    * @param bordered       Add vertical borders.
    * @param borderless     Removes all borders.
    * @param hover          Highlight row on hover.
    * @param small          Makes table more compact.
    * @param componentId    An id of the root DOM node.
    * @param rowFactory     Creates row representation of the table element - it should create the `tr` tag.
    *                       Use the provided interceptor to properly clean up bindings inside the content.
    * @param headerFactory  Creates table header - it should create the `tr` tag.
    *                       Table without header will be rendered if `None` passed.
    *                       Use the provided interceptor to properly clean up bindings inside the content.
    * @param captionFactory Creates table caption - the result will be wrapped into the `caption` tag.
    *                       Table without caption will be rendered if `None` passed.
    *                       Use the provided interceptor to properly clean up bindings inside the content.
    * @tparam ItemType A single element's type in the `items` sequence.
    * @tparam ElemType A type of a property containing an element in the `items` sequence.
    * @return A `ExtTable` component, call `render` to create a DOM element.
    */
  def apply[ItemType, ElemType <: ReadableProperty[ItemType]](
    items: seq.ReadableSeqProperty[ItemType, ElemType],
    responsive: ReadableProperty[Option[BootstrapStyles.ResponsiveBreakpoint]]  = UdashBootstrap.None,
    dark: ReadableProperty[Boolean] = UdashBootstrap.False,
    striped: ReadableProperty[Boolean] = UdashBootstrap.False,
    bordered: ReadableProperty[Boolean] = UdashBootstrap.False,
    borderless: ReadableProperty[Boolean] = UdashBootstrap.False,
    hover: ReadableProperty[Boolean] = UdashBootstrap.False,
    small: ReadableProperty[Boolean] = UdashBootstrap.False,
    componentId: ComponentId = ComponentId.newId()
  )(
    rowFactory: (ElemType, Binding.NestedInterceptor) => Element,
    headerFactory: Option[Binding.NestedInterceptor => Modifier] = None,
    captionFactory: Option[Binding.NestedInterceptor => Modifier] = None
  ): ExtTable[ItemType, ElemType] = {
    new ExtTable(
      items, responsive, dark, striped, bordered, borderless, hover, small, componentId
    )(captionFactory, headerFactory, rowFactory)
  }

  @js.annotation.JSExport
  var callback: js.Function1[JQuery, Unit] = _

}
