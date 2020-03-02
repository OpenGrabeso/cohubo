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

  case class TableAttrib[ItemType](
    name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Modifier,
    style: Option[String] = None,
    modifier: Option[ItemType => Modifier] = None,
    shortName: Option[String] = None
  )

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      val st = a.style.map(style := _)
      a.shortName.map { shortName =>
        val wide = th(s.wideMedia, b(a.name), st).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = th(st, s.narrowMedia, b(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(st, b(a.name)).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator, SelType](
    id: ItemType => SelType,
    indent: ItemType => Int,
    sel: Property[Option[SelType]], attribs: Seq[TableAttrib[ItemType]]
  ): (CastableProperty[ItemType], NestedInterceptor) => Element = { (el,_) =>
    val level = indent(el.get)
    tr(
      s.tr,
      produceWithNested(el) { (ha, nested) =>
        attribs.flatMap { a =>
          // existing but empty shortName means the column should be hidden on narrow view
          val tdItem = td(s.td, a.modifier.map(_ (ha)), a.value(ha, el.asModel, nested))
          if (a.shortName.contains("")) {
            tdItem(s.wideMedia).render
          } else {
            tdItem.render
          }
        }
      },

      `class` := "table-fold",
      attr("data-depth") := level,

      onclick :+= { e: Event =>
        val td = e.target.asInstanceOf[Element]
        // e.target may be a td inside of tr, we need to find a tr parent in such case

        val tr = jQ(td).closest("tr")
        val wasSelected = tr.hasClass("selected")
        if (!wasSelected) {
          val selId = id(el.get)
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

        // TODO: separate folding control (using the checkbox)

        // from https://stackoverflow.com/a/49364929/16673
        //println(tr.attr("data-depth"))

        // find all children (following items with greater level)
        def findChildren(tr: JQuery) = {
          def getDepth(d: Option[Any]) = d.map(_.asInstanceOf[Int]).getOrElse(0)
          val depth = getDepth(tr.data("depth"))
          tr.nextUntil(jQ("tr").filter((x: Element, _: Int, _: Element) => {
            getDepth(jQ(x).data("depth")) <= depth
          }))
        }

        val children = findChildren(jQ(tr))
        //println(children.length)

        if (jQ(children).is(":visible")) {
          jQ(tr).addClass("closed")
          jQ(children).hide()
        } else {
          jQ(tr).removeClass("closed")
          jQ(children).show()
          val ch = findChildren(jQ(".closed"))
          jQ(ch).hide()
        }
        false
      }
    ).render
  }
}
