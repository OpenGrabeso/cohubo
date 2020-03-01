package com.github.opengrabeso.cohabo
package frontend.views

import common.css._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom.{Element, Event, Node}
import io.udash.css.CssView._
import io.udash.properties.ModelPropertyCreator
import io.udash.wrappers.jquery.jQ
import scalatags.JsDom.all._

object TableFactory {
  val s = SelectPageStyles

  case class TableAttrib[ItemType](name: String, value: (ItemType, ModelProperty[ItemType], NestedInterceptor) => Seq[Node], shortName: Option[String] = None)

  def headerFactory[ItemType](attribs: Seq[TableAttrib[ItemType]]): NestedInterceptor => Modifier = _ => tr {
    attribs.flatMap { a =>
      a.shortName.map { shortName =>
        val wide = td(s.wideMedia, b(a.name)).render
        if (shortName.isEmpty) {
          Seq(wide)
        } else {
          val narrow = td(s.narrowMedia, b(a.shortName)).render
          Seq(wide, narrow)
        }
      }.getOrElse(Seq(th(b(a.name)).render))
    }
  }.render

  def rowFactory[ItemType: ModelPropertyCreator](id: ItemType => String, sel: Property[String], attribs: Seq[TableAttrib[ItemType]]): (CastableProperty[ItemType], NestedInterceptor) => Element = { (el,_) =>
    tr(
      s.tr,
      produceWithNested(el) { (ha, nested) =>
        attribs.flatMap { a =>
          // existing but empty shortName means the column should be hidden on narrow view
          if (a.shortName.contains("")) {
            td(s.td, s.wideMedia, a.value(ha, el.asModel, nested)).render
          } else {
            td(s.td, a.value(ha, el.asModel, nested)).render
          }
        }
      },
      onclick :+= { e: Event =>
        val td = e.target.asInstanceOf[Element]
        // e.target may be a td inside of tr, we need to find a tr parent in such case

        val tr = jQ(td).closest("tr")
        val wasSelected = tr.hasClass("selected")
        if (!wasSelected) {
          val selId = id(el.get)
          tr.addClass("selected")
          sel.set(selId)

          // once the selection is changed from us, unselect us
          def listenCallback(newId: String): Unit = {
            if (newId == selId) sel.listenOnce(listenCallback) // listen again
            else {
              tr.removeClass("selected")
            }
          }
          sel.listenOnce(listenCallback)
        }

        false
      }
    ).render
  }
}
