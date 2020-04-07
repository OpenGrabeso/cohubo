package com.github.opengrabeso.cohabo
package frontend.views

import common.css._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom.{Element, Event, Node}
import io.udash.css.CssView._
import io.udash.properties.ModelPropertyCreator
import io.udash.wrappers.jquery.{JQuery, jQ}
import scalatags.JsDom.all._

object TableFactory {
  val s = SelectPageStyles

  object symbols {
    val childrenPreview = "\u2299" // (.) circled dot
    val childrenOpen = "\u02c5" // v modifier letter down
    val childrenClosed = "\u02c3" // > modifier letter right arrowhead
    val noChildren = "\u22A1" // |.| squared dot operator
    val childrenLoading = "\u2A02" // (x) circled times operator
  }

  case class TableAttrib[ItemType](
    name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Modifier,
    style: Option[String] = None,
    modifier: Option[ItemType => Modifier] = None,
    shortName: Option[String] = None
  )

  trait TableRowHandler[ItemType, SelType] {
    def id(item: ItemType): SelType
    def indent(item: ItemType): Int
    def rowModifier(itemModel: ModelProperty[ItemType]): Modifier
    def tdModifier: Modifier
    def rowModifyElement(element: Element): Unit
  }

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      val st = a.style.map(style := _)
      a.shortName.map { shortName =>
        val wide = th(s.th, s.wideMedia, div(a.name), st).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = th(s.th, st, s.narrowMedia, div(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(s.th, st, div(a.name)).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator, SelType](
    selDisabled: ReadableProperty[Boolean],
    sel: Property[Option[SelType]],
    attribs: Seq[TableAttrib[ItemType]]
  )(implicit rowHandler: TableRowHandler[ItemType, SelType]): (CastableProperty[ItemType], NestedInterceptor) => Element = { (el,_) =>
    val timing = false
    val start = System.currentTimeMillis()
    val elData = el.get
    val level = rowHandler.indent(elData)
    val row = tr(
      rowHandler.rowModifier(el.asModel),
      produceWithNested(el) { (ha, nested) =>
        attribs.flatMap { a =>
          // existing but empty shortName means the column should be hidden on narrow view
          val tdItem = td(rowHandler.tdModifier, a.modifier.map(_ (ha)), a.value(ha, el.asModel, nested))
          if (a.shortName.contains("")) {
            tdItem(s.wideMedia).render
          } else {
            tdItem.render
          }
        }
      },

      if (level > 0) Seq[Modifier](style := "display: none") else Seq.empty[Modifier],
      attr("data-depth") := level,

      onclick :+= { e: Event =>
        if (!selDisabled.get) {
          val td = e.target.asInstanceOf[Element]
          // e.target may be a td inside of tr, we need to find a tr parent in such case
          val tr = jQ(td).closest("tr")
          val wasSelected = tr.hasClass("selected")
          if (!wasSelected) {
            val selId = rowHandler.id(elData)
            tr.addClass("selected")
            sel.set(Some(selId))

            // once the selection is changed from us, unselect us
            def listenCallback(newId: Option[SelType]): Unit = {
              if (newId.contains(selId)) sel.listenOnce(listenCallback) // listen again
              else {
                tr.removeClass("selected")
              }
            }
            sel.listenOnce(listenCallback)
          }
        }

        false
      }
    ).render

    rowHandler.rowModifyElement(row)

    if (timing) println(s"Row $elData took ${System.currentTimeMillis()-start}")
    row
  }
}
